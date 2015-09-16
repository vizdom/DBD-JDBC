#!/bin/bash
set -euo pipefail

OLDPWD=$(pwd)
cd DBD-JDBC
cpanm -v --notest --installdeps .
cd $OLDPWD

