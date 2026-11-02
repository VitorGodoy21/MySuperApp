import ctypes
from ctypes import wintypes
import copy
import json
from pathlib import Path
import re
import subprocess
import tempfile
import threading
import uuid
from urllib.parse import parse_qs, urlsplit


def video_url(value):
    if not isinstance(value, str) or len(value) > 2048:
        raise ValueError("Informe um link de video do YouTube.")
    try:
        url = urlsplit(value.strip())
        query = parse_qs(url.query, keep_blank_values=True)
        if (url.scheme != "https" or url.username or url.password
                or url.port not in (None, 443) or "list" in query):
            raise ValueError()
        if url.hostname == "youtu.be":
            video_id = url.path.removeprefix("/")
        elif url.hostname in ("youtube.com", "www.youtube.com", "m.youtube.com"):
            if url.path == "/watch":
                ids = query.get("v", [])
                video_id = ids[0] if len(ids) == 1 else ""
            elif url.path.startswith(("/shorts/", "/embed/")):
                video_id = url.path.split("/")[-1]
            else:
                raise ValueError()
        else:
            raise ValueError()
        if not re.fullmatch(r"[A-Za-z0-9_-]{11}", video_id):
            raise ValueError()
    except (ValueError, TypeError):
        raise ValueError("Use um link HTTPS de video individual do YouTube, sem playlist.") from None
    return f"https://www.youtube.com/watch?v={video_id}"


def playlist_url(value):
    try:
        if not isinstance(value, str) or len(value) > 2048:
            raise ValueError()
        url = urlsplit(value.strip())
        query = parse_qs(url.query, keep_blank_values=True)
        ids = query.get("list", [])
        if (url.scheme != "https" or url.username or url.password or url.port not in (None, 443)
                or len(ids) != 1):
            raise ValueError()
        if url.hostname == "youtu.be":
            video_url(f"https://youtu.be{url.path}")
        elif url.hostname in ("youtube.com", "www.youtube.com", "m.youtube.com"):
            if url.path == "/watch":
                video_url(f"https://www.youtube.com/watch?v={query.get('v', [''])[0]}")
            elif url.path != "/playlist":
                raise ValueError()
        else:
            raise ValueError()
        playlist_id = ids[0]
        if not re.fullmatch(r"(?:PL|UU|FL|OLAK5uy_)[A-Za-z0-9_-]{1,100}", playlist_id):
            raise ValueError()
    except (ValueError, TypeError):
        raise ValueError("Use uma playlist publica do YouTube. Mixes, radio e listas dinamicas nao sao suportados.") from None
    return f"https://www.youtube.com/playlist?list={playlist_id}", playlist_id


def safe_name(title, limit=90):
    name = re.sub(r'[<>:"/\\|?*\x00-\x1f]', "_", str(title))[:limit].rstrip(" .")
    if not name or name in (".", ".."):
        name = "Sem titulo"
    if name.split(".")[0].upper() in {"CON", "PRN", "AUX", "NUL", *(f"COM{i}" for i in range(1, 10)), *(f"LPT{i}" for i in range(1, 10))}:
        name = "_" + name
    return name


class Cancelled(Exception):
    pass


class ProcessTree:
    """The job kills descendants too, including a running FFmpeg conversion."""

    def __init__(self):
        self.kernel = ctypes.WinDLL("kernel32", use_last_error=True)
        self.kernel.CreateJobObjectW.restype = wintypes.HANDLE
        self.kernel.CreateJobObjectW.argtypes = [ctypes.c_void_p, wintypes.LPCWSTR]
        self.kernel.AssignProcessToJobObject.argtypes = [wintypes.HANDLE, wintypes.HANDLE]
        self.kernel.SetInformationJobObject.argtypes = [wintypes.HANDLE, ctypes.c_int,
                                                       ctypes.c_void_p, wintypes.DWORD]
        self.kernel.TerminateJobObject.argtypes = [wintypes.HANDLE, wintypes.UINT]
        self.kernel.CloseHandle.argtypes = [wintypes.HANDLE]
        self.handle = self.kernel.CreateJobObjectW(None, None)
        if not self.handle:
            raise ctypes.WinError(ctypes.get_last_error())
        class BasicLimit(ctypes.Structure):
            _fields_ = [
                ("PerProcessUserTimeLimit", ctypes.c_longlong),
                ("PerJobUserTimeLimit", ctypes.c_longlong),
                ("LimitFlags", wintypes.DWORD),
                ("MinimumWorkingSetSize", ctypes.c_size_t),
                ("MaximumWorkingSetSize", ctypes.c_size_t),
                ("ActiveProcessLimit", wintypes.DWORD),
                ("Affinity", ctypes.c_size_t),
                ("PriorityClass", wintypes.DWORD),
                ("SchedulingClass", wintypes.DWORD),
            ]
        class ExtendedLimit(ctypes.Structure):
            _fields_ = [
                ("BasicLimitInformation", BasicLimit),
                ("IoInfo", ctypes.c_ulonglong * 6),
                ("ProcessMemoryLimit", ctypes.c_size_t),
                ("JobMemoryLimit", ctypes.c_size_t),
                ("PeakProcessMemoryUsed", ctypes.c_size_t),
                ("PeakJobMemoryUsed", ctypes.c_size_t),
            ]
        limits = ExtendedLimit()
        limits.BasicLimitInformation.LimitFlags = 0x2000  # JOB_OBJECT_LIMIT_KILL_ON_JOB_CLOSE
        if not self.kernel.SetInformationJobObject(self.handle, 9, ctypes.byref(limits), ctypes.sizeof(limits)):
            self.kernel.CloseHandle(self.handle)
            raise ctypes.WinError(ctypes.get_last_error())
        self.lock = threading.Lock()

    def assign(self, process, suspended=False):
        with self.lock:
            if not self.handle or not self.kernel.AssignProcessToJobObject(self.handle, int(process._handle)):
                process.kill()
                process.wait()
                raise OSError("Nao foi possivel controlar o processo de download.")
            if suspended:
                # Assign before resuming so no child can escape the job during startup.
                ntdll = ctypes.WinDLL("ntdll")
                ntdll.NtResumeProcess.argtypes = [wintypes.HANDLE]
                if ntdll.NtResumeProcess(int(process._handle)) != 0:
                    self.kernel.TerminateJobObject(self.handle, 1)
                    raise OSError("Nao foi possivel iniciar o processo de download.")

    def terminate(self):
        with self.lock:
            if self.handle:
                self.kernel.TerminateJobObject(self.handle, 1)

    def close(self):
        with self.lock:
            if self.handle:
                self.kernel.TerminateJobObject(self.handle, 1)
                self.kernel.CloseHandle(self.handle)
                self.handle = None


class Downloader:
    def __init__(self, vendor, folder):
        self.vendor = Path(vendor)
        self.folder = str(folder)
        self.lock = threading.RLock()
        self.state = {"phase": "idle", "message": "Pronto para baixar.", "operation": "",
                      "mode": "video", "format": "mp3", "height": 720}
        self.items = []
        self.worker = None
        self.cancelled = threading.Event()
        self.closed = False
        self.tree = None

    def status(self):
        with self.lock:
            counts = {key: 0 for key in ("done", "failed", "skipped", "cancelled", "unavailable")}
            for item in self.items:
                if item["phase"] in counts:
                    counts[item["phase"]] += 1
            selected = sum(item["selected"] for item in self.items)
            return copy.deepcopy({**self.state, "folder": self.folder, "total": len(self.items),
                                  "selected": selected, "counts": counts,
                                  "busy": bool(self.worker and self.worker.is_alive()),
                                  "retryable": sum(item["phase"] in ("failed", "cancelled") for item in self.items)})

    def update(self, **values):
        with self.lock:
            self.state.update(values)

    def _idle(self):
        if self.closed or (self.worker and self.worker.is_alive()):
            raise ValueError("Aguarde a operacao atual terminar.")

    def _operation(self, operation):
        if not isinstance(operation, str) or operation != self.state["operation"]:
            raise ValueError("A lista foi substituida. Recarregue a interface.")

    def page(self, operation, page=0):
        with self.lock:
            self._operation(operation)
            if type(page) is not int or page < 0:
                raise ValueError("Pagina invalida.")
            start = page * 50
            return {"operation": operation, "page": page, "total": len(self.items),
                    "items": copy.deepcopy(self.items[start:start + 50])}

    def select(self, operation, action, indices=None, selected=True):
        with self.lock:
            self._operation(operation)
            self._idle()
            if self.state["phase"] != "ready":
                raise ValueError("Carregue uma playlist antes de selecionar.")
            if action in ("all", "none"):
                targets = range(len(self.items))
                selected = action == "all"
            elif (action == "items" and isinstance(indices, list) and len(indices) <= 50
                  and all(type(i) is int and 0 <= i < len(self.items) for i in indices)
                  and type(selected) is bool):
                targets = indices
            else:
                raise ValueError("Selecao invalida.")
            for i in targets:
                self.items[i]["selected"] = selected and self.items[i]["phase"] != "unavailable"

    def reset(self, operation):
        with self.lock:
            self._operation(operation)
            self._idle()
            self.items = []
            self.state = {"operation": uuid.uuid4().hex, "mode": "video", "format": "mp3",
                          "height": 720, "phase": "idle", "message": "Pronto para baixar."}

    def output_folder(self):
        with self.lock:
            return self.state.get("destination", self.folder)

    def set_folder(self, folder):
        with self.lock:
            self._idle()
            if not Path(folder).is_dir():
                raise ValueError("Escolha uma pasta existente.")
            self.folder = str(folder)
            if self.state["phase"] == "ready":
                self.state["destination"] = str(Path(self.folder) / self.state["subfolder"])

    def _launch(self, target, *args):
        self.cancelled.clear()
        self.worker = threading.Thread(target=target, args=args, daemon=False)
        self.worker.start()

    def start(self, url, format="mp3", height=720):
        url = video_url(url)
        if format not in ("mp3", "mp4") or type(height) is not int or height not in (360, 720, 1080):
            raise ValueError("Escolha MP3 ou MP4 e resolucao de 360p, 720p ou 1080p.")
        with self.lock:
            self._idle()
            self.state = {"operation": uuid.uuid4().hex, "mode": "video", "format": format,
                          "height": height, "phase": "processing", "message": "Preparando...",
                          "destination": self.folder, "url": url}
            self.items = [self._item(url.rsplit("=", 1)[1], "Preparando video...", 0)]
            self._launch(self._queue, [0], self.folder, format, height)

    @staticmethod
    def _item(video_id, title, index, available=True):
        return {"index": index, "id": video_id, "title": title, "selected": available,
                "phase": "pending" if available else "unavailable", "message": "",
                "percent": None, "filename": ""}

    def preview(self, value):
        url, playlist_id = playlist_url(value)
        with self.lock:
            self._idle()
            self.items = []
            self.state = {"operation": uuid.uuid4().hex, "mode": "playlist", "format": "mp3",
                          "height": 720, "phase": "loading_playlist", "message": "Carregando playlist...",
                          "url": url, "playlist_id": playlist_id, "title": playlist_id}
            self._launch(self._preview, url, playlist_id)

    def _preview(self, url, playlist_id):
        try:
            def receive(line):
                if not line.startswith("{"):
                    return
                entry = json.loads(line)
                video_id = entry.get("id")
                valid_id = isinstance(video_id, str) and bool(re.fullmatch(r"[A-Za-z0-9_-]{11}", video_id))
                title = str(entry.get("title") or "Video indisponivel")[:300]
                available = valid_id and entry.get("availability") not in ("private", "premium_only", "subscriber_only") and title not in ("[Private video]", "[Deleted video]")
                with self.lock:
                    if not self.items:
                        self.state["title"] = str(entry.get("playlist_title") or playlist_id)[:300]
                    self.items.append(self._item(video_id if valid_id else "", title, len(self.items), available))
            code = self._execute(self._base(playlist=True) + [
                "--flat-playlist", "--lazy-playlist", "--dump-json", "--skip-download",
                "--", url], receive)
            self._check_cancelled()
            if code or not self.items:
                raise ValueError("Nao foi possivel carregar a playlist, ou ela esta vazia. Verifique se e publica.")
            with self.lock:
                # The ID distinguishes playlists whose sanitized titles coincide.
                subfolder = safe_name(self.state["title"], 65) + f" [{playlist_id}]"
                self.state.update(phase="ready", message="Selecione os itens para baixar.",
                                  subfolder=subfolder, destination=str(Path(self.folder) / subfolder))
        except Cancelled:
            self.update(phase="cancelled", message="Carregamento cancelado. Carregue a lista novamente.")
        except Exception as error:
            self.update(phase="error", message=str(error))

    def start_playlist(self, operation):
        with self.lock:
            self._operation(operation)
            self._idle()
            if self.state["mode"] != "playlist" or self.state["phase"] != "ready":
                raise ValueError("Carregue uma playlist antes de baixar.")
            indices = [i for i, item in enumerate(self.items) if item["selected"]]
            if not indices:
                raise ValueError("Selecione ao menos um item disponivel.")
            folder = self.state["destination"]
            self.state.update(phase="processing", message="Iniciando fila...")
            self._launch(self._queue, indices, folder, "mp3", 720)

    def retry(self, operation):
        with self.lock:
            self._operation(operation)
            self._idle()
            if self.state["phase"] not in ("completed", "cancelled", "error"):
                raise ValueError("Aguarde a fila terminar.")
            indices = [i for i, item in enumerate(self.items) if item["phase"] in ("failed", "cancelled")]
            if not indices:
                raise ValueError("Nao ha falhas ou cancelados para repetir.")
            for i in indices:
                self.items[i].update(phase="pending", message="", percent=None)
            self.state.update(phase="processing", message="Repetindo itens...")
            self._launch(self._queue, indices, self.state["destination"],
                         self.state["format"], self.state["height"])

    def _base(self, playlist=False):
        return [
            str(self.vendor / "yt-dlp.exe"), "--ignore-config",
            "--yes-playlist" if playlist else "--no-playlist",
            "--use-extractors", "youtube.*" if playlist else "youtube", "--no-plugin-dirs",
            "--js-runtimes", f"deno:{self.vendor / 'deno.exe'}",
            "--ffmpeg-location", str(self.vendor), "--no-update",
            "--socket-timeout", "20", "--retries", "2", "--fragment-retries", "2",
            "--no-colors", "--encoding", "utf-8",
        ]

    @staticmethod
    def format_args(format, height):
        if format == "mp3":
            return ["-f", "bestaudio/best", "-x", "--audio-format", "mp3", "--audio-quality", "192K"]
        selection = (
            f"bv[ext=mp4][vcodec^=avc1][height<={height}]+ba[ext=m4a][acodec^=mp4a]/"
            f"b[ext=mp4][vcodec^=avc1][acodec^=mp4a][height<={height}]"
        )
        return ["-f", selection, "--merge-output-format", "mp4", "--remux-video", "mp4"]

    def arguments(self, url, work, format="mp3", height=720):
        return self._base() + self.format_args(format, height) + [
            "--no-overwrites", "--newline", "--no-colors",
            "--progress-template", 'download:PROGRESS:%(progress._percent_str)s',
            "--print", "after_move:FILE:%(filepath)j",
            "--no-simulate", "-o", str(Path(work) / "media.%(ext)s"),
            "--", url,
        ]

    def _check_cancelled(self):
        if self.cancelled.is_set():
            raise Cancelled()

    def _execute(self, arguments, receive):
        process = None
        tree = ProcessTree()
        try:
            with self.lock:
                self._check_cancelled()
                self.tree = tree
                process = subprocess.Popen(
                    arguments, stdout=subprocess.PIPE, stderr=subprocess.STDOUT,
                    stdin=subprocess.DEVNULL, text=True, encoding="utf-8", errors="replace",
                    creationflags=subprocess.CREATE_NO_WINDOW | 0x4,
                )
                tree.assign(process, suspended=True)
            for line in process.stdout:
                self._check_cancelled()
                receive(line.strip())
            code = process.wait()
            self._check_cancelled()
            return code
        finally:
            tree.close()
            if process:
                process.wait()
                process.stdout.close()
            with self.lock:
                self.tree = None

    def _metadata(self, url, format, height):
        result = []
        code = self._execute(self._base() + self.format_args(format, height) + [
            "--dump-single-json", "--skip-download", "--", url],
            lambda line: result.append(json.loads(line)) if line.startswith("{") else None)
        if code or not result:
            raise ValueError("Video indisponivel, acesso bloqueado ou formato compativel ausente. Tente outro link autorizado ou atualize o auxiliar.")
        info = result[-1]
        if info.get("is_live") or info.get("live_status") in ("is_live", "is_upcoming"):
            raise ValueError("Transmissoes ao vivo em andamento ou agendadas nao sao suportadas.")
        return info

    def _item_update(self, index, **values):
        with self.lock:
            self.items[index].update(values)
            self.state["current"] = copy.deepcopy(self.items[index])

    def _download(self, index, folder, format, height):
        item = self.items[index]
        url = video_url(f"https://www.youtube.com/watch?v={item['id']}")
        info = self._metadata(url, format, height)
        title = str(info.get("title") or item["title"])
        filename = f"{safe_name(title)} [{item['id']}].{format}"
        destination = Path(folder) / filename
        self._item_update(index, title=title[:300], filename=filename, height=info.get("height"))
        if destination.exists():
            return "skipped", "Arquivo existente, sem sobrescrita."
        with tempfile.TemporaryDirectory(prefix=".mysuperapp-", dir=folder) as work:
            output = None

            def receive(line):
                nonlocal output
                if line.startswith("PROGRESS:"):
                    match = re.search(r"(\d+(?:\.\d+)?)%", line)
                    self._item_update(index, phase="downloading",
                                      message="Baixando faixa atual..." if format == "mp4" else "Baixando audio...",
                                      percent=min(100, float(match[1])) if match else None)
                elif line.startswith(("[ExtractAudio]", "[Merger]", "[VideoRemuxer]")):
                    self._item_update(index, phase="converting" if format == "mp3" else "merging",
                                      message="Convertendo audio..." if format == "mp3" else "Juntando faixas sem recodificar...",
                                      percent=None)
                elif line.startswith("FILE:"):
                    output = Path(json.loads(line[5:])).resolve()

            code = self._execute(self.arguments(url, work, format, height), receive)
            if (code or not output or output.parent != Path(work).resolve()
                    or output.suffix.lower() != "." + format or not output.is_file() or output.stat().st_size == 0):
                raise ValueError("Nao foi possivel concluir o arquivo. Verifique a rede, o acesso ao video e o formato escolhido.")
            probe = []
            probe_code = self._execute([
                str(self.vendor / "ffprobe.exe"), "-v", "error", "-show_entries",
                "stream=codec_name,codec_type,height", "-of", "json", str(output)],
                lambda line: probe.append(line))
            streams = json.loads("\n".join(probe)).get("streams", []) if probe_code == 0 else []
            if format == "mp3":
                valid = any(s.get("codec_name") == "mp3" for s in streams)
            else:
                videos = [s for s in streams if s.get("codec_type") == "video"]
                audios = [s for s in streams if s.get("codec_type") == "audio"]
                valid = (bool(videos) and bool(audios)
                         and all(s.get("codec_name") == "h264" and 0 < s.get("height", 0) <= height for s in videos)
                         and all(s.get("codec_name") == "aac" for s in audios))
            if not valid:
                raise ValueError("O arquivo nao corresponde ao formato solicitado.")
            with self.lock:
                self._check_cancelled()
                try:
                    output.rename(destination)
                except FileExistsError:
                    return "skipped", "Arquivo existente, sem sobrescrita."
            return "done", "Arquivo salvo."

    def _queue(self, indices, folder, format, height):
        try:
            for name in ("yt-dlp.exe", "ffmpeg.exe", "ffprobe.exe", "deno.exe"):
                if not (self.vendor / name).is_file():
                    raise OSError("Instalacao incompleta. Reinstale o aplicativo.")
            Path(folder).mkdir(parents=False, exist_ok=True)
            for i in indices:
                self._check_cancelled()
                self._item_update(i, phase="preparing", message="Verificando video...", percent=None)
                try:
                    phase, message = self._download(i, folder, format, height)
                    self._item_update(i, phase=phase, message=message, percent=None)
                except Cancelled:
                    raise
                except Exception as error:
                    self._item_update(i, phase="failed", message=str(error), percent=None)
            self._check_cancelled()
            self.update(phase="completed", message="Processamento encerrado. Confira o resumo e o resultado de cada item.")
        except Cancelled:
            self.update(phase="cancelled", message="Fila cancelada. Arquivos concluidos foram preservados.")
        except Exception as error:
            self.update(phase="error", message=str(error))
        finally:
            with self.lock:
                for i in indices:
                    if self.items[i]["phase"] in ("pending", "preparing", "downloading", "converting", "merging"):
                        self.items[i].update(phase="cancelled" if self.cancelled.is_set() else "failed",
                                             message="Cancelado." if self.cancelled.is_set() else self.state["message"],
                                             percent=None)
                self.state.pop("current", None)

    def cancel(self):
        with self.lock:
            self.cancelled.set()
            if self.tree:
                self.tree.terminate()

    def close(self):
        with self.lock:
            self.closed = True
        self.cancel()
        if self.worker:
            self.worker.join(timeout=30)
