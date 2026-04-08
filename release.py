#!/usr/bin/env python3
import os
import sys
import json
import shutil
import zipfile
import subprocess
import tempfile

VERSION = sys.argv[1] if len(sys.argv) > 1 else None
if not VERSION:
    print("Usage: ./release.py <version> (e.g. ./release.py vx.x.x)")
    sys.exit(1)

REPO_ROOT = os.path.dirname(os.path.abspath(__file__))
SUBMODULES = os.path.join(REPO_ROOT, "submodules")
SIMULATOR_JAR = f"{SUBMODULES}/de.persosim.simulator/de.persosim.simulator/target/de.persosim.simulator-0.19.0-SNAPSHOT.jar"
FRAGMENT_E4XMI = os.path.join(REPO_ROOT, "patches", "fragment.e4xmi")
ZIP_NAME = f"persosim-sqm-patch-{VERSION}.zip"

CLASSES = [
    "de/persosim/simulator/protocols/file/AbstractFileProtocol.class",
    "de/persosim/simulator/PersoSim.class",
]

PATCH = {
    "version": VERSION,
    "actions": [
        {
            "type": "copy_class",
            "class": "de/persosim/simulator/protocols/file/AbstractFileProtocol.class",
            "destination": "plugins/de.persosim.simulator_0.18.3.20220209"
        },
        {
            "type": "copy_class",
            "class": "de/persosim/simulator/PersoSim.class",
            "destination": "plugins/de.persosim.simulator_0.18.3.20220209"
        },
        {
            "type": "patch_jar",
            "file": "fragment.e4xmi",
            "destination": "plugins",
            "jar": "de.persosim.simulator.ui_0.18.3.20220209.jar"
        }
    ]
}

# Step 1 - Build
print("Building simulator...")
result = subprocess.run(
    ["mvn", "clean", "install", "-DskipTests", "-f",
     f"{SUBMODULES}/de.persosim.simulator/de.persosim.simulator/pom.xml"],
    capture_output=False
)
if result.returncode != 0:
    print("Build failed, aborting.")
    sys.exit(1)

# Step 2 - Package
print("Packaging...")
with tempfile.TemporaryDirectory() as tmp:
    # Extract class files from JAR
    for cls in CLASSES:
        subprocess.run(["unzip", "-o", SIMULATOR_JAR, cls, "-d", f"{tmp}/classes"], check=True)

    # Copy fragment.e4xmi
    os.makedirs(f"{tmp}/files", exist_ok=True)
    shutil.copy(FRAGMENT_E4XMI, f"{tmp}/files/fragment.e4xmi")

    # Write patch.json
    with open(f"{tmp}/patch.json", "w") as f:
        json.dump(PATCH, f, indent=2)

    # Create zip
    with zipfile.ZipFile(ZIP_NAME, "w") as zf:
        for cls in CLASSES:
            zf.write(f"{tmp}/classes/{cls}", f"classes/{cls}")
        zf.write(f"{tmp}/files/fragment.e4xmi", "files/fragment.e4xmi")
        zf.write(f"{tmp}/patch.json", "patch.json")

print(f"Created {ZIP_NAME}")

# Step 3 - Upload to GitHub
upload = input("Upload to GitHub releases? (y/n): ")
if upload.lower() == "y":
    subprocess.run(["gh", "release", "create", VERSION, ZIP_NAME, "--title", VERSION], check=True)
    print("Released!")
else:
    print(f"Zip saved locally as {ZIP_NAME}")
