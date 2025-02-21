#!/bin/bash

echo garbage collecting...
find ${ZIP_CACHEPATH}/* -mtime +1 -exec rm -v {} \;
echo done