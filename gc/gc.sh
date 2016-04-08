#!/bin/bash

echo garbage collecting...
find ${ZIP_CACHEPATH}/* -mtime +14 -exec rm -v {} \;
echo done