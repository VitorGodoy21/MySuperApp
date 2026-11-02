import ctypes
from ctypes import wintypes
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
import json
import os
from pathlib import Path
import queue
import secrets
import sys
import threading
import tkinter as tk
from tkinter import filedialog, messagebox, ttk
import webbrowser
from urllib.parse import parse_qs, urlsplit

from downloader import Downloader


ROOT = Path(getattr(sys, "_MEIPASS", Path(__file__).parent))


def music_folder():
    # FOLDERID_Music supports Windows folder redirection.
    import uuid
    guid = (ctypes.c_byte * 16).from_buffer_copy(uuid.UUID("4bd8d571-6d19-48d3-be97-422220080e43").bytes_le)
    result = ctypes.c_wchar_p()
    shell = ctypes.WinDLL("shell32")
    shell.SHGetKnownFolderPath.argtypes = [ctypes.c_void_p, wintypes.DWORD, wintypes.HANDLE,
                                          ctypes.POINTER(ctypes.c_wchar_p)]
    code = shell.SHGetKnownFolderPath(ctypes.byref(guid), 0x8000, None, ctypes.byref(result))
    if code != 0:
        raise OSError("Nao foi possivel localizar sua pasta Musicas.")
    folder = result.value
    ole = ctypes.WinDLL("ole32")
    ole.CoTaskMemFree.argtypes = [ctypes.c_void_p]
    ole.CoTaskMemFree(ctypes.cast(result, ctypes.c_void_p))
    return folder


class LocalServer(ThreadingHTTPServer):
    daemon_threads = True

    def __init__(self, engine, actions):
        self.engine = engine
        self.actions = actions
        self.token = secrets.token_urlsafe(32)
        super().__init__(("127.0.0.1", 0), Handler)
        self.origin = f"http://127.0.0.1:{self.server_port}"
        self.host = f"127.0.0.1:{self.server_port}"

    @property
    def launch_url(self):
        return self.origin + "/#" + self.token


class Handler(BaseHTTPRequestHandler):
    def setup(self):
        super().setup()
        self.connection.settimeout(10)

    def log_message(self, *_):
        pass

    def send(self, code, payload, content_type="application/json; charset=utf-8"):
        data = payload if isinstance(payload, bytes) else json.dumps(payload).encode()
        self.send_response(code)
        self.send_header("Content-Type", content_type)
        self.send_header("Content-Length", str(len(data)))
        self.send_header("Cache-Control", "no-store")
        self.send_header("X-Content-Type-Options", "nosniff")
        self.send_header("Referrer-Policy", "no-referrer")
        self.send_header("Content-Security-Policy", "default-src 'none'; script-src 'self'; style-src 'self'; connect-src 'self'; frame-ancestors 'none'; base-uri 'none'; form-action 'none'")
        self.end_headers()
        self.wfile.write(data)

    def allowed(self, authenticated=True):
        if self.headers.get("Host") != self.server.host:
            return False
        origin = self.headers.get("Origin")
        if origin is not None and origin != self.server.origin:
            return False
        if self.headers.get("Sec-Fetch-Site") not in (None, "same-origin", "none"):
            return False
        return not authenticated or secrets.compare_digest(
            self.headers.get("X-Local-Token", "").encode("utf-8"), self.server.token.encode("utf-8"))

    def do_GET(self):
        url = urlsplit(self.path)
        if not self.allowed(authenticated=url.path.startswith("/api/")):
            self.send(403, {"error": "Acesso local nao autorizado."})
            return
        if self.path == "/api/status":
            self.send(200, self.server.engine.status())
            return
        if url.path == "/api/items":
            try:
                args = parse_qs(url.query, strict_parsing=True)
                if set(args) != {"operation", "page"} or any(len(v) != 1 for v in args.values()):
                    raise ValueError()
                self.send(200, self.server.engine.page(args["operation"][0], int(args["page"][0])))
            except ValueError:
                self.send(400, {"error": "Lista ou pagina invalida. Atualize a interface."})
            return
        files = {"/": ("index.html", "text/html; charset=utf-8"),
                 "/app.js": ("app.js", "text/javascript; charset=utf-8"),
                 "/style.css": ("style.css", "text/css; charset=utf-8")}
        if self.path not in files:
            self.send(404, {"error": "Nao encontrado."})
            return
        name, kind = files[self.path]
        self.send(200, (ROOT / "public" / name).read_bytes(), kind)

    def do_POST(self):
        if not self.allowed() or self.headers.get("Origin") != self.server.origin:
            self.send(403, {"error": "Acesso local nao autorizado."})
            return
        try:
            length = int(self.headers.get("Content-Length", "0"))
            if not 0 < length <= 4096 or self.headers.get("Content-Type") != "application/json":
                self.send(400, {"error": "Requisicao invalida."})
                return
            payload = json.loads(self.rfile.read(length))
            if not isinstance(payload, dict):
                raise ValueError("Requisicao invalida.")
            if self.path == "/api/start":
                if payload.get("authorized") is not True:
                    raise ValueError("Confirme que possui autorizacao para baixar o conteudo.")
                if payload.get("mode", "video") == "playlist":
                    if payload.get("format", "mp3") != "mp3":
                        raise ValueError("Playlists suportam somente MP3.")
                    self.server.engine.start_playlist(payload.get("operation"))
                elif payload.get("mode", "video") == "video":
                    self.server.engine.start(payload.get("url"), payload.get("format", "mp3"), payload.get("height", 720))
                else:
                    raise ValueError("Modo invalido.")
            elif self.path == "/api/preview":
                self.server.engine.preview(payload.get("url"))
            elif self.path == "/api/select":
                self.server.engine.select(payload.get("operation"), payload.get("action"),
                                          payload.get("indices"), payload.get("selected", True))
            elif self.path == "/api/reset":
                self.server.engine.reset(payload.get("operation"))
            elif self.path == "/api/retry":
                if payload.get("authorized") is not True:
                    raise ValueError("Confirme que possui autorizacao para baixar o conteudo.")
                self.server.engine.retry(payload.get("operation"))
            elif self.path == "/api/cancel":
                self.server.engine.cancel()
            elif self.path in ("/api/folder", "/api/open-folder", "/api/exit"):
                self.server.actions.put(self.path)
            else:
                self.send(404, {"error": "Nao encontrado."})
                return
            self.send(202, {"ok": True})
        except (ValueError, UnicodeError) as error:
            message = str(error) if type(error) is ValueError else "Requisicao invalida."
            self.send(400, {"error": message})
        except Exception:
            self.send(500, {"error": "Nao foi possivel executar esta operacao."})


def single_instance():
    import getpass
    import hashlib
    kernel = ctypes.WinDLL("kernel32", use_last_error=True)
    kernel.CreateMutexW.restype = wintypes.HANDLE
    kernel.CreateMutexW.argtypes = [ctypes.c_void_p, wintypes.BOOL, wintypes.LPCWSTR]
    name = hashlib.sha256(getpass.getuser().encode()).hexdigest()[:24]
    mutex = kernel.CreateMutexW(None, False, "Local\\MySuperAppDownloader-" + name)
    if not mutex:
        raise ctypes.WinError(ctypes.get_last_error())
    exists = ctypes.get_last_error() == 183
    return kernel, mutex, exists


def main():
    if sys.argv[1:] not in ([], ["mysuperapp-downloader://open"], ["mysuperapp-downloader://open/"]):
        return
    root = tk.Tk()
    root.withdraw()
    kernel, mutex, exists = single_instance()
    # The named event signals the first instance without sharing its HTTP token.
    kernel.CreateEventW.restype = wintypes.HANDLE
    kernel.CreateEventW.argtypes = [ctypes.c_void_p, wintypes.BOOL, wintypes.BOOL, wintypes.LPCWSTR]
    kernel.SetEvent.argtypes = [wintypes.HANDLE]
    kernel.WaitForSingleObject.argtypes = [wintypes.HANDLE, wintypes.DWORD]
    kernel.CloseHandle.argtypes = [wintypes.HANDLE]
    import getpass
    event = kernel.CreateEventW(None, False, False, "Local\\MySuperAppDownloader-Open-" + getpass.getuser())
    if not event:
        kernel.CloseHandle(mutex)
        root.destroy()
        raise ctypes.WinError(ctypes.get_last_error())
    if exists:
        kernel.SetEvent(event)
        kernel.CloseHandle(event)
        kernel.CloseHandle(mutex)
        root.destroy()
        return
    engine = None
    server = None
    try:
        engine = Downloader(ROOT / "vendor", music_folder())
        actions = queue.Queue()
        server = LocalServer(engine, actions)
        threading.Thread(target=server.serve_forever, daemon=True).start()
        root.title("My Super App - Downloader")
        root.geometry("440x230")
        root.resizable(False, False)
        frame = ttk.Frame(root, padding=24)
        frame.pack(fill="both", expand=True)
        ttk.Label(frame, text="Downloader MP3 e MP4", font=("Segoe UI", 16, "bold")).pack(anchor="w")
        ttk.Label(frame, text="Controle os downloads pela interface no navegador.\nFechar esta janela encerra o aplicativo e o download.",
                  wraplength=390).pack(anchor="w", pady=16)

        def open_browser():
            webbrowser.open(server.launch_url)

        def shutdown():
            engine.close()
            server.shutdown()
            root.destroy()

        ttk.Button(frame, text="Abrir interface", command=open_browser).pack(side="left")
        ttk.Button(frame, text="Encerrar", command=shutdown).pack(side="right")
        root.protocol("WM_DELETE_WINDOW", shutdown)

        def poll():
            if kernel.WaitForSingleObject(event, 0) == 0:
                open_browser()
            try:
                while True:
                    action = actions.get_nowait()
                    if action == "/api/exit":
                        shutdown()
                        return
                    if action == "/api/folder":
                        folder = filedialog.askdirectory(parent=root, initialdir=engine.folder,
                                                         title="Escolha onde salvar os arquivos")
                        if folder:
                            engine.set_folder(folder)
                    elif action == "/api/open-folder":
                        folder = engine.output_folder()
                        if not Path(folder).is_dir():
                            folder = engine.folder
                        os.startfile(folder)
            except queue.Empty:
                pass
            except Exception:
                messagebox.showerror("Downloader", "Nao foi possivel abrir ou selecionar a pasta.", parent=root)
            root.after(150, poll)

        root.deiconify()
        root.after(150, poll)
        open_browser()
        root.mainloop()
    except Exception as error:
        messagebox.showerror("Downloader", str(error), parent=root)
    finally:
        if engine:
            engine.close()
        if server:
            server.server_close()
        kernel.CloseHandle(event)
        kernel.CloseHandle(mutex)


if __name__ == "__main__":
    main()
