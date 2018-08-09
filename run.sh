#!/bin/bash

COMMAND="java -cp"
CONTROLLER=target/ChelDesktop-1.0-SNAPSHOT-jar-with-dependencies.jar 

case "$1" in
"index")
  OP="ie.adaptcentre.chel.KnowledgeBaseIndexer"
  ;;
"cli")
  OP="ie.adaptcentre.chel.CommandLineInterface"
  ;;
"web")
  chmod +x target/freme-package/bin/start_local.sh
  COMMAND=./target/freme-package/bin/start_local.sh
  CONTROLLER=""
  OP=""
  ;;
*)
  echo "Invalid operation $1"
  exit 1
  ;;
esac

echo "$COMMAND" "$CONTROLLER" "$OP" "${@:2}"
$COMMAND "$CONTROLLER" "$OP" "${@:2}"
