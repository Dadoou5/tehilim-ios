.PHONY: help setup fetch project open test clean

PYTHON ?= python3
SIMULATOR ?= iPhone 17

# Use local xcodegen if present, otherwise fall back to PATH (brew install)
XCODEGEN := $(shell test -x .tools/xcodegen/bin/xcodegen && echo .tools/xcodegen/bin/xcodegen || echo xcodegen)

help:
	@echo "Targets:"
	@echo "  make setup    — install Python deps + xcodegen via Homebrew"
	@echo "  make fetch    — download 150 Psalms (HE+FR) into data/psalms.json"
	@echo "  make project  — (re)generate Tehilim.xcodeproj from project.yml"
	@echo "  make open     — open the Xcode project"
	@echo "  make test     — run unit tests on $(SIMULATOR)"
	@echo "  make clean    — delete generated Xcode project"

setup:
	$(PYTHON) -m pip install --user -r scripts/requirements.txt
	@command -v xcodegen >/dev/null 2>&1 || ( \
		echo "Installing xcodegen via Homebrew..." && \
		brew install xcodegen )

fetch:
	$(PYTHON) scripts/fetch_psalms.py

project:
	$(XCODEGEN) generate

open: project
	open Tehilim.xcodeproj

test: project
	xcodebuild test \
		-project Tehilim.xcodeproj \
		-scheme Tehilim \
		-destination 'platform=iOS Simulator,name=$(SIMULATOR)'

clean:
	rm -rf Tehilim.xcodeproj
