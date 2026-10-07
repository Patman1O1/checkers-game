#!/bin/sh

mvn clean install
mvn -pl "$1" javafx:run