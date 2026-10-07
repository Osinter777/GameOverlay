# Safe antivirus heuristic test
# This file is intentionally harmless. It does NOT download, execute, delete,
# encrypt, persist, or modify anything outside this script.

import base64
import os
import subprocess

encoded_text = "SGVsbG8sIGFudGl2aXJ1cyB0ZXN0IQ=="
decoded_text = base64.b64decode(encoded_text).decode("utf-8")

suspicious_names = [
    "powershell",
    "cmd.exe",
    "download",
    "payload",
    "execute",
    "persistence",
]

def harmless_test():
    # Only creates an in-memory string and prints it.
    joined = " ".join(suspicious_names)
    print(decoded_text)
    print(joined)

if __name__ == "__main__":
    harmless_test()
