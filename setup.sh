#!/bin/bash

checkinstallmodule() {
    local module=$1

    perl -M${module} -e '1' 2>/dev/null
    perlmrc=$?

    if [[ ${perlmrc} != 0 ]]; then
        echo "Installing missing ${module} module."
        perl -MCPAN -e "install ${module}"
    fi
}

## Require XMLParser for XML::Simple
checkinstallmodule XML::Parser

## Require XML::SAX,XML::SAX::Expat,XML::NamespaceSupport for XML::Simple
checkinstallmodule XML::SAX
checkinstallmodule XML::SAX::Expat
checkinstallmodule XML::NamespaceSupport

## Require XML::Simple
checkinstallmodule XML::Simple

## Convert::BER
checkinstallmodule Convert::BER

# Obviously.
checkinstallmodule DBI
