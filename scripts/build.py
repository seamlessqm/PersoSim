#!/usr/bin/env python3
import os
import sys
import subprocess

REPO_ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SUBMODULES = os.path.join(REPO_ROOT, "submodules")


def build_all(passes=3):
    for i in range(passes):
        print(f"\n=== Pass {i+1}/{passes} ===")
        for repo in sorted(os.listdir(SUBMODULES)):
            repo_path = os.path.join(SUBMODULES, repo)
            pom = os.path.join(repo_path, repo, "pom.xml")
            if os.path.isfile(pom):
                result = subprocess.run(
                    ["mvn", "clean", "install", "-DskipTests", "-f", pom],
                    capture_output=True, text=True
                )
                status = "SUCCESS" if result.returncode == 0 else "FAILURE"
                print(f"{repo}: {status}")


build_all()
