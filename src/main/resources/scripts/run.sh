#!/bin/bash

CONTROLLER=${project.build.directory}/${project.build.finalName}-${assembly.descriptorRef}.${project.packaging}

case "$1" in
"index")
  OP="${project.groupId}.KnowledgeBaseIndexer"
  ;;
"cli")
  OP="${project.groupId}.CommandLineInterface"
  ;;
*)
  echo "Invalid operation $1"
  exit 1
  ;;
esac

java -cp "$CONTROLLER" "$OP" "${@:2}"
