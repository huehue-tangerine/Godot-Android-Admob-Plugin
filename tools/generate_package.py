#!/usr/bin/env python3
"""Build and package a new GodotAdMob plugin release."""

from __future__ import annotations

import argparse
import re
import subprocess
import sys
from pathlib import Path


VERSION_PATTERN = re.compile(r"^\d+\.\d+\.\d+$")
PLUGIN_CODE_PATTERN = re.compile(r"(ext\.pluginVersionCode\s*=\s*)\d+")
PLUGIN_NAME_PATTERN = re.compile(
    r'(ext\.pluginVersionName\s*=\s*)"[^"]*"'
)
GDAP_BINARY_PATTERN = re.compile(
    r'(binary\s*=\s*"GodotAdMob\.)[^"]+(\.release\.aar")'
)


def run(command: list[str], cwd: Path) -> None:
    print(f"Running: {' '.join(command)}")
    subprocess.run(command, cwd=cwd, check=True)


def replace_once(path: Path, pattern: re.Pattern[str], replacement: str) -> None:
    content = path.read_text(encoding="utf-8")
    updated, count = pattern.subn(replacement, content, count=1)
    if count != 1:
        raise RuntimeError(f"Could not update expected value in {path}")
    path.write_text(updated, encoding="utf-8")


def update_version(root: Path, version: str) -> None:
    version_file = root / "admob-plugin" / "version.gradle"
    gdap_file = root / "config" / "GodotAdMob.gdap"

    version_content = version_file.read_text(encoding="utf-8")
    gdap_content = gdap_file.read_text(encoding="utf-8")
    code_match = PLUGIN_CODE_PATTERN.search(version_content)
    if code_match is None:
        raise RuntimeError(f"Could not find pluginVersionCode in {version_file}")

    current_code = int(re.search(r"\d+", code_match.group(0)).group())
    current_name_match = re.search(
        r'ext\.pluginVersionName\s*=\s*"([^"]+)"', version_content
    )
    if current_name_match is None:
        raise RuntimeError(f"Could not find pluginVersionName in {version_file}")

    updated_version_content, name_count = PLUGIN_NAME_PATTERN.subn(
        rf'\g<1>"{version}"', version_content, count=1
    )
    if name_count != 1:
        raise RuntimeError(f"Could not update pluginVersionName in {version_file}")

    if current_name_match.group(1) != version:
        updated_version_content, code_count = PLUGIN_CODE_PATTERN.subn(
            rf"\g<1>{current_code + 1}", updated_version_content, count=1
        )
        if code_count != 1:
            raise RuntimeError(f"Could not update pluginVersionCode in {version_file}")

    updated_gdap_content, binary_count = GDAP_BINARY_PATTERN.subn(
        rf"\g<1>{version}\g<2>", gdap_content, count=1
    )
    if binary_count != 1:
        raise RuntimeError(f"Could not update binary in {gdap_file}")

    version_file.write_text(updated_version_content, encoding="utf-8")
    gdap_file.write_text(updated_gdap_content, encoding="utf-8")


def main() -> int:
    parser = argparse.ArgumentParser(
        description="Update, build and package a GodotAdMob plugin release."
    )
    parser.add_argument("version", help="Release version, for example 7.0.3")
    args = parser.parse_args()

    if not VERSION_PATTERN.fullmatch(args.version):
        parser.error("version must use the format MAJOR.MINOR.PATCH")

    tools_dir = Path(__file__).resolve().parent
    root = tools_dir.parent
    gradle_dir = root / "admob-plugin"
    gradlew = gradle_dir / "gradlew.bat"
    package_script = tools_dir / "package_build.sh"

    for required in (gradlew, package_script):
        if not required.is_file():
            raise FileNotFoundError(f"Required file not found: {required}")

    update_version(root, args.version)
    print(f"Updated plugin files to version {args.version}")

    run([str(gradlew), ":godotadmob:assembleRelease", "--no-daemon"], gradle_dir)
    run(["bash", package_script.name, args.version], tools_dir)
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except subprocess.CalledProcessError as error:
        print(f"Command failed with exit code {error.returncode}", file=sys.stderr)
        raise
