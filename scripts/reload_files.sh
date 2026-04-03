#!/bin/bash
# Usage: ./reload_files.sh Class1.class Class2.class ...

JAR=~/zone51/de.persosim.simulator/de.persosim.simulator/target/de.persosim.simulator-0.19.0-SNAPSHOT.jar
INSTALL=/home/joseph/de.persosim.rcp.product-0.18.3-20220209/plugins/de.persosim.simulator_0.18.3.20220209/

echo "Building..."
mvn clean install -DskipTests -f ~/zone51/de.persosim.simulator/de.persosim.simulator/pom.xml

if [ $? -ne 0 ]; then
    echo "Build failed, aborting."
    exit 1
fi

for CLASS in "$@"; do
    echo "Deploying $CLASS..."
    unzip -o $JAR $CLASS -d $INSTALL
done

echo "Done! Relaunch PersoSim to apply changes."
