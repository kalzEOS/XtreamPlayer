#!/usr/bin/env bash
set -euo pipefail

tag_name="${1:-}"
release_title="${2:-}"
release_notes_file="${3:-}"
is_prerelease="${4:-false}"
target_branch="${5:-}"

if [[ -z "${tag_name}" ]]; then
  echo "Missing tag name"
  exit 1
fi

if [[ -z "${release_title}" ]]; then
  echo "Missing release title"
  exit 1
fi

if [[ ! -f "${release_notes_file}" ]]; then
  echo "Release notes file does not exist: ${release_notes_file}"
  exit 1
fi

if [[ ! "${tag_name}" =~ ^XtreamPlayerv([0-9]+)\.([0-9]+)(\.([0-9]+))?(-rc[0-9]+)?$ ]]; then
  echo "Invalid tag format: ${tag_name}"
  echo "Expected: XtreamPlayervx.x or XtreamPlayervx.x.x (optional -rcN)"
  exit 1
fi

version="${tag_name#XtreamPlayerv}"
expected_title="Xtream Player v${version}"
if [[ "${release_title}" != "${expected_title}" ]]; then
  echo "Invalid release title: ${release_title}"
  echo "Expected: ${expected_title}"
  exit 1
fi

first_non_empty_line="$(grep -m1 -E '\S' "${release_notes_file}" || true)"
if [[ "${first_non_empty_line}" != "# Changes:" ]]; then
  echo "Release notes must start with: # Changes:"
  exit 1
fi

if [[ "${is_prerelease}" == "true" ]]; then
  if [[ "${version}" != *-rc* ]]; then
    echo "Pre-release must use an rc tag suffix (example: -rc1)."
    exit 1
  fi
else
  if [[ "${version}" == *-rc* ]]; then
    echo "Production release cannot use an rc suffix."
    exit 1
  fi
  if [[ -n "${target_branch}" && "${target_branch}" != "main" ]]; then
    echo "Production release target must be main, got: ${target_branch}"
    exit 1
  fi
fi

echo "Release metadata validation passed for ${tag_name}"
