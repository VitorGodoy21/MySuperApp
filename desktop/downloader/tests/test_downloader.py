import ctypes
from ctypes import wintypes
import io
import json
import os
from pathlib import Path
import subprocess
import tempfile
import threading
import time
import sys
import unittest
from unittest.mock import patch
from urllib.error import HTTPError
from urllib.request import Request, urlopen
import queue

from app import LocalServer
from downloader import Downloader, ProcessTree, playlist_url, safe_name, video_url


class VideoUrlTest(unittest.TestCase):
    def test_allowed_urls_are_canonicalized(self):
        for url in ("https://youtu.be/Abcdef_12-3?t=10",
                    "https://www.youtube.com/watch?v=Abcdef_12-3&si=example",
                    "https://m.youtube.com/shorts/Abcdef_12-3"):
            self.assertEqual(video_url(url), "https://www.youtube.com/watch?v=Abcdef_12-3")

    def test_rejects_nonvideo_playlist_and_command_inputs(self):
        for url in (None, {}, "--exec calc", "file:///C:/test.mp3",
                    "http://youtu.be/Abcdef_12-3", "https://youtube.com.evil.test/watch?v=Abcdef_12-3",
                    "https://youtube.com@evil.test/watch?v=Abcdef_12-3",
                    "https://youtube.com:8080/watch?v=Abcdef_12-3",
                    "https://youtube.com/playlist?list=x",
                    "https://youtube.com/watch?v=Abcdef_12-3&list=x",
                    "https://youtu.be/Abcdef_12-3?list=",
                    "https://youtube.com/watch?v=Abcdef_12-3&v=aaaaaaaaaaa",
                    "https://youtu.be/../../test", "https://youtu.be/Abcdef_12-3/extra"):
            with self.subTest(url=url), self.assertRaises(ValueError):
                video_url(url)

    def test_playlist_urls_canonicalized_and_mixes_rejected(self):
        for url in ("https://www.youtube.com/playlist?list=PLabc123",
                    "https://youtu.be/Abcdef_12-3?list=PLabc123",
                    "https://www.youtube.com/watch?v=Abcdef_12-3&list=PLabc123"):
            self.assertEqual(playlist_url(url), ("https://www.youtube.com/playlist?list=PLabc123", "PLabc123"))
        for url in ("https://youtube.com/playlist?list=RDabc123",
                    "https://youtube.com/playlist?list=PLabc&list=PLdef",
                    "https://evil.test/playlist?list=PLabc",
                    "https://youtube.com/channel/test?list=PLabc",
                    "https://youtube.com/playlist?list=PL" + "a" * 101,
                    "https://youtube.com/playlist?list=PL../../x", None):
            with self.subTest(url=url), self.assertRaises(ValueError):
                playlist_url(url)

    def test_safe_windows_names(self):
        for title in ("../../x", "CON", "NUL.mp3", 'A:B\\C/<x>?*', "", ".."):
            result = safe_name(title)
            self.assertNotRegex(result, r'[<>:"/\\|?*\x00-\x1f]')
            self.assertNotIn(result, ("", ".", "..", "CON"))
        self.assertLessEqual(len(safe_name("a" * 1000)), 90)


class EngineTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.folder = Path(self.temp.name)
        self.vendor = self.folder / "tools"
        self.vendor.mkdir()
        for name in ("yt-dlp.exe", "ffmpeg.exe", "ffprobe.exe", "deno.exe"):
            (self.vendor / name).touch()
        self.engine = Downloader(self.vendor, self.folder)

    def tearDown(self):
        self.engine.close()
        self.temp.cleanup()

    def execute(self, args, receive):
        if args[0].endswith("ffprobe.exe"):
            receive(json.dumps({"streams": [{"codec_type": "audio", "codec_name": "mp3"}]}))
            return 0
        if "--dump-single-json" in args:
            receive(json.dumps({"title": "Example", "id": "Abcdef_12-3"}))
            return 0
        self.assertIn("--no-playlist", args)
        self.assertIn("--ignore-config", args)
        self.assertEqual(args[args.index("--audio-quality") + 1], "192K")
        self.assertEqual(args[-2], "--")
        work = Path(args[args.index("-o") + 1]).parent
        output = work / "media.mp3"
        output.write_bytes(b"mp3-fixture")
        for line in ['PROGRESS: 50.0%', '[ExtractAudio] Destination: file', 'FILE:' + json.dumps(str(output))]:
            receive(line)
        return 0

    def test_success_and_collision_preserve_user_file_and_clean_temporary(self):
        with patch.object(self.engine, "_execute", side_effect=self.execute) as execute:
            self.engine.start("https://youtu.be/Abcdef_12-3")
            self.engine.worker.join(5)
            self.assertEqual(self.engine.status()["counts"]["done"], 1)
            saved = self.folder / "Example [Abcdef_12-3].mp3"
            self.assertEqual(saved.read_bytes(), b"mp3-fixture")
            saved.write_bytes(b"keep-user-file")
            self.engine.start("https://youtu.be/Abcdef_12-3")
            self.engine.worker.join(5)
            self.assertEqual(self.engine.status()["counts"]["skipped"], 1)
            # Second run skips after metadata, before downloading media again.
            self.assertEqual(sum("-o" in call.args[0] for call in execute.call_args_list), 1)
            self.assertEqual(saved.read_bytes(), b"keep-user-file")
            self.assertEqual(list(self.folder.glob(".mysuperapp-*")), [])

    def test_failure_is_not_reported_as_success(self):
        with patch.object(self.engine, "_execute", return_value=1):
            self.engine.start("https://youtu.be/Abcdef_12-3")
            self.engine.worker.join(5)
            self.assertEqual(self.engine.status()["counts"]["failed"], 1)
            self.assertFalse(list(self.folder.glob("*.mp3")))
            self.assertFalse(list(self.folder.glob(".mysuperapp-*")))

    def test_concurrent_start_and_folder_change_rejected(self):
        gate = threading.Event()
        def waiting(*args, **kwargs):
            gate.wait(5)
        with patch.object(self.engine, "_queue", side_effect=waiting):
            self.engine.start("https://youtu.be/Abcdef_12-3")
            try:
                with self.assertRaises(ValueError):
                    self.engine.start("https://youtu.be/Abcdef_12-3")
                with self.assertRaises(ValueError):
                    self.engine.set_folder(self.folder)
            finally:
                gate.set()
                self.engine.worker.join(5)

    def test_cancel_does_not_move_output(self):
        def cancelled(args, receive):
            result = self.execute(args, receive)
            if "-o" in args:
                self.engine.cancel()
            return result
        with patch.object(self.engine, "_execute", side_effect=cancelled):
            self.engine.start("https://youtu.be/Abcdef_12-3")
            self.engine.worker.join(5)
            self.assertEqual(self.engine.status()["phase"], "cancelled")
            self.assertEqual(self.engine.status()["counts"]["cancelled"], 1)
            self.assertFalse(list(self.folder.glob("*.mp3")))

    def preview(self):
        def entries(args, receive):
            self.assertIn("--flat-playlist", args)
            self.assertIn("--lazy-playlist", args)
            for entry in [
                {"id": "Abcdef_12-3", "title": "First", "playlist_title": "My: Playlist"},
                {"id": "aaaaaaaaaaa", "title": "Second"},
                {"id": "bbbbbbbbbbb", "title": "[Private video]", "availability": "private"},
                {"id": "Abcdef_12-3", "title": "Duplicate"},
            ]:
                receive(json.dumps(entry))
            return 0
        with patch.object(self.engine, "_execute", side_effect=entries):
            self.engine.preview("https://youtube.com/playlist?list=PLtest123")
            self.engine.worker.join(5)
        return self.engine.status()["operation"]

    def test_preview_selection_and_pagination_are_versioned(self):
        operation = self.preview()
        state = self.engine.status()
        self.assertEqual(state["phase"], "ready")
        self.assertEqual(state["selected"], 3)
        self.assertIn("My_ Playlist [PLtest123]", state["destination"])
        self.engine.select(operation, "none")
        with self.assertRaises(ValueError):
            self.engine.start_playlist(operation)
        self.engine.select(operation, "all")
        self.engine.select(operation, "items", [1], False)
        page = self.engine.page(operation)
        self.assertEqual(len(page["items"]), 4)
        self.assertFalse(page["items"][2]["selected"])
        page["items"][0]["phase"] = "hacked"
        self.assertEqual(self.engine.page(operation)["items"][0]["phase"], "pending")
        for args in ((operation, "items", [999], True), (operation, "items", [True], True),
                     ("stale", "all", None, True)):
            with self.assertRaises(ValueError):
                self.engine.select(*args)
        for i in range(120):
            self.engine.items.append(self.engine._item("Abcdef_12-3", str(i), i + 4))
        self.assertEqual(len(self.engine.page(operation, 1)["items"]), 50)
        self.assertEqual(len(self.engine.page(operation, 2)["items"]), 24)
        self.assertNotIn("items", self.engine.status())

    def test_queue_continues_after_failure_and_retry_only_eligible(self):
        operation = self.preview()
        calls = []
        def download(index, folder, format, height):
            calls.append(index)
            if index == 1:
                raise ValueError("unavailable")
            return ("skipped", "exists") if index == 3 else ("done", "saved")
        with patch.object(self.engine, "_download", side_effect=download):
            self.engine.start_playlist(operation)
            self.engine.worker.join(5)
        self.assertEqual(calls, [0, 1, 3])
        self.assertEqual(self.engine.status()["counts"], {"done": 1, "failed": 1, "skipped": 1, "cancelled": 0, "unavailable": 1})
        calls.clear()
        with patch.object(self.engine, "_download", side_effect=lambda *args: (calls.append(args[0]) or ("done", "saved"))):
            self.engine.retry(operation)
            self.engine.worker.join(5)
        self.assertEqual(calls, [1])
        self.assertEqual(self.engine.status()["counts"]["done"], 2)

    def test_cancel_between_items_preserves_done_and_cancels_pending(self):
        operation = self.preview()
        def download(*args):
            self.engine.cancel()
            return "done", "saved"
        with patch.object(self.engine, "_download", side_effect=download):
            self.engine.start_playlist(operation)
            self.engine.worker.join(5)
        self.assertEqual(self.engine.status()["counts"]["done"], 1)
        self.assertEqual(self.engine.status()["counts"]["cancelled"], 2)
        self.assertEqual(self.engine.status()["phase"], "cancelled")
        self.assertEqual(self.engine.page(operation)["items"][2]["phase"], "unavailable")

    def test_preview_failure_and_cancel_do_not_enable_partial_download(self):
        def partial(args, receive):
            receive(json.dumps({"id": "Abcdef_12-3", "title": "example"}))
            return 1
        with patch.object(self.engine, "_execute", side_effect=partial):
            self.engine.preview("https://youtube.com/playlist?list=PLtest123")
            self.engine.worker.join(5)
        self.assertEqual(self.engine.status()["phase"], "error")
        with self.assertRaises(ValueError):
            self.engine.start_playlist(self.engine.status()["operation"])
        def cancel(args, receive):
            self.engine.cancel()
            return 0
        with patch.object(self.engine, "_execute", side_effect=cancel):
            self.engine.preview("https://youtube.com/playlist?list=PLtest123")
            self.engine.worker.join(5)
        self.assertEqual(self.engine.status()["phase"], "cancelled")

    def test_live_and_invalid_format_rejected_without_media_download(self):
        for format, height in (("exe", 720), ("mp4", 2160), ("mp4", True), ({}, 720)):
            with self.assertRaises(ValueError):
                self.engine.start("https://youtu.be/Abcdef_12-3", format, height)
        with patch.object(self.engine, "_execute", side_effect=lambda args, receive: (receive('{"title":"Live","is_live":true}') or 0)) as execute:
            self.engine.start("https://youtu.be/Abcdef_12-3")
            self.engine.worker.join(5)
            self.assertEqual(execute.call_count, 1)
            self.assertEqual(self.engine.status()["counts"]["failed"], 1)

    def test_mp4_codec_selection_and_final_probe(self):
        for height in (360, 720, 1080):
            args = self.engine.arguments("https://youtu.be/Abcdef_12-3", self.folder, "mp4", height)
            selection = args[args.index("-f") + 1]
            self.assertIn(f"height<={height}", selection)
            self.assertIn("vcodec^=avc1", selection)
            self.assertIn("acodec^=mp4a", selection)
            self.assertNotIn("--recode-video", args)
            self.assertNotIn("-x", args)
        def mp4(args, receive):
            if "--dump-single-json" in args:
                receive('{"title":"Movie","height":360}')
            elif args[0].endswith("ffprobe.exe"):
                receive('{"streams":[{"codec_type":"video","codec_name":"h264","height":360},{"codec_type":"audio","codec_name":"aac"}]}')
            else:
                output = Path(args[args.index("-o") + 1]).parent / "media.mp4"
                output.write_bytes(b"fixture")
                receive("FILE:" + json.dumps(str(output)))
            return 0
        with patch.object(self.engine, "_execute", side_effect=mp4):
            self.engine.start("https://youtu.be/Abcdef_12-3", "mp4", 720)
            self.engine.worker.join(5)
        self.assertEqual(self.engine.status()["counts"]["done"], 1)
        self.assertTrue((self.folder / "Movie [Abcdef_12-3].mp4").exists())

    def test_mp4_wrong_codec_or_excess_resolution_never_saved(self):
        for codec, height in (("av1", 360), ("h264", 1080)):
            with self.subTest(codec=codec, height=height):
                def output(args, receive):
                    if "--dump-single-json" in args:
                        receive('{"title":"Movie"}')
                    elif args[0].endswith("ffprobe.exe"):
                        receive(json.dumps({"streams": [
                            {"codec_type": "video", "codec_name": codec, "height": height},
                            {"codec_type": "audio", "codec_name": "aac"},
                        ]}))
                    else:
                        media = Path(args[args.index("-o") + 1]).parent / "media.mp4"
                        media.write_bytes(b"fixture")
                        receive("FILE:" + json.dumps(str(media)))
                    return 0
                with patch.object(self.engine, "_execute", side_effect=output):
                    self.engine.start("https://youtu.be/Abcdef_12-3", "mp4", 720)
                    self.engine.worker.join(5)
                self.assertEqual(self.engine.status()["counts"]["failed"], 1)
                self.assertFalse(list(self.folder.glob("*.mp4")))
                self.assertFalse(list(self.folder.glob(".mysuperapp-*")))

    @unittest.skipUnless(os.name == "nt", "Windows process-tree control")
    def test_windows_job_terminates_process(self):
        tree = ProcessTree()
        process = subprocess.Popen(
            ["ping", "-n", "30", "127.0.0.1"], stdout=subprocess.DEVNULL,
            creationflags=subprocess.CREATE_NO_WINDOW | 0x4,
        )
        try:
            tree.assign(process, suspended=True)
            tree.terminate()
            self.assertNotEqual(process.wait(timeout=5), 0)
        finally:
            tree.close()
            if process.poll() is None:
                process.kill()

    @unittest.skipUnless(os.name == "nt", "Windows process-tree control")
    def test_windows_job_close_terminates_descendant(self):
        pid_file = self.folder / "child.pid"
        script = (
            "import subprocess,time,pathlib;"
            "p=subprocess.Popen(['ping','-n','30','127.0.0.1'],stdout=subprocess.DEVNULL);"
            f"pathlib.Path({str(pid_file)!r}).write_text(str(p.pid));"
            "time.sleep(30)"
        )
        tree = ProcessTree()
        parent = subprocess.Popen([sys.executable, "-c", script],
                                  creationflags=subprocess.CREATE_NO_WINDOW | 0x4)
        try:
            tree.assign(parent, suspended=True)
            for _ in range(50):
                if pid_file.exists():
                    break
                time.sleep(.05)
            self.assertTrue(pid_file.exists())
            kernel = ctypes.WinDLL("kernel32", use_last_error=True)
            kernel.OpenProcess.argtypes = [wintypes.DWORD, wintypes.BOOL, wintypes.DWORD]
            kernel.OpenProcess.restype = wintypes.HANDLE
            kernel.WaitForSingleObject.argtypes = [wintypes.HANDLE, wintypes.DWORD]
            kernel.CloseHandle.argtypes = [wintypes.HANDLE]
            child = kernel.OpenProcess(0x100000, False, int(pid_file.read_text()))
            self.assertTrue(child)
            try:
                tree.close()
                parent.wait(timeout=5)
                self.assertEqual(kernel.WaitForSingleObject(child, 5000), 0)
            finally:
                kernel.CloseHandle(child)
        finally:
            tree.close()
            if parent.poll() is None:
                parent.kill()


class LocalHttpTest(unittest.TestCase):
    def setUp(self):
        self.engine = unittest.mock.Mock()
        self.engine.status.return_value = {"phase": "idle", "folder": "example"}
        self.actions = queue.Queue()
        self.server = LocalServer(self.engine, self.actions)
        self.thread = threading.Thread(target=self.server.serve_forever)
        self.thread.start()

    def tearDown(self):
        self.server.shutdown()
        self.server.server_close()
        self.thread.join()

    def request(self, path, body=None, headers=None):
        default = {"X-Local-Token": self.server.token}
        if body is not None:
            default.update({"Content-Type": "application/json", "Origin": self.server.origin})
        default.update(headers or {})
        data = json.dumps(body).encode() if body is not None else None
        try:
            return urlopen(Request(self.server.origin + path, data=data, headers=default), timeout=3)
        except HTTPError as error:
            return error

    def test_authenticated_start_and_private_status(self):
        with self.request("/api/start", {"url": "https://youtu.be/Abcdef_12-3", "authorized": True}) as response:
            self.assertEqual(response.status, 202)
        self.engine.start.assert_called_once_with("https://youtu.be/Abcdef_12-3", "mp3", 720)
        with self.request("/api/status") as response:
            self.assertEqual(json.load(response)["phase"], "idle")
            self.assertEqual(response.headers["Cache-Control"], "no-store")
            self.assertNotIn("Access-Control-Allow-Origin", response.headers)

    def test_cross_origin_host_token_and_consent_are_enforced(self):
        payload = {"url": "https://youtu.be/Abcdef_12-3", "authorized": True}
        for headers in ({"Origin": "https://evil.test"}, {"Host": "evil.test"},
                        {"X-Local-Token": ""}, {"Sec-Fetch-Site": "cross-site"},
                        {"Content-Type": "text/plain"}):
            with self.subTest(headers=headers), self.request("/api/start", payload, headers) as response:
                self.assertIn(response.status, (400, 403))
        with self.request("/api/start", {"url": payload["url"]}) as response:
            self.assertEqual(response.status, 400)
        self.engine.start.assert_not_called()
        with self.request("/api/status", headers={"X-Local-Token": ""}) as response:
            self.assertEqual(response.status, 403)

    def test_no_arbitrary_file_or_command_endpoint(self):
        for path in ("/../app.py", "/vendor/yt-dlp.exe", "/api/exec", "/?file=C:"):
            with self.request(path) as response:
                self.assertEqual(response.status, 404)
        with self.request("/") as response:
            self.assertEqual(response.status, 200)
            self.assertIn("frame-ancestors 'none'", response.headers["Content-Security-Policy"])
        with self.request("/api/folder", {}) as response:
            self.assertEqual(response.status, 202)
        self.assertEqual(self.actions.get_nowait(), "/api/folder")

    def test_playlist_routes_and_mp4_contract(self):
        with self.request("/api/preview", {"url": "https://youtube.com/playlist?list=PLabc"}) as response:
            self.assertEqual(response.status, 202)
        self.engine.preview.assert_called_once()
        with self.request("/api/start", {"mode": "playlist", "format": "mp4", "operation": "abc", "authorized": True}) as response:
            self.assertEqual(response.status, 400)
        self.engine.start_playlist.assert_not_called()
        with self.request("/api/start", {"mode": "playlist", "operation": "abc", "authorized": True}) as response:
            self.assertEqual(response.status, 202)
        self.engine.start_playlist.assert_called_once_with("abc")
        with self.request("/api/retry", {"operation": "abc", "authorized": False}) as response:
            self.assertEqual(response.status, 400)
        self.engine.page.return_value = {"operation": "abc", "items": []}
        with self.request("/api/items?operation=abc&page=0") as response:
            self.assertEqual(response.status, 200)
        with self.request("/api/items?operation=abc&page=0", headers={"X-Local-Token": ""}) as response:
            self.assertEqual(response.status, 403)
        with self.request("/api/items?operation=abc&page=bad") as response:
            self.assertEqual(response.status, 400)


if __name__ == "__main__":
    unittest.main()
