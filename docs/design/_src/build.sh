#!/bin/sh
# Builds the artifact body (no document wrapper) and a standalone local copy.
cd "$(dirname "$0")"
PARTS="1-head.html 2-mock.css 3-body-a.html 3-body-b.html 3-body-c.html 4-data.js 5a-mocks.js 5b-mocks.js 5c-mocks.js 6a-content.js 6b-engine.js"
cat $PARTS > ../artifact.html
{ printf '<!doctype html>\n<html lang="zh-CN">\n<head>\n<meta charset="utf-8">\n<meta name="viewport" content="width=device-width, initial-scale=1, viewport-fit=cover">\n</head>\n<body>\n'; cat $PARTS; printf '\n</body>\n</html>\n'; } > ../bv-expressive-redesign.html
