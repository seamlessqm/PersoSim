#!/usr/bin/env
for jar in linux/plugins/de.persosim.simulator.basics_1.4.0.20251112.jar linux/plugins/de.persosim.simulator.controller_1.4.0.20251112 linux/plugins/de.persosim.rcp_1.4.0.20251112.jar; do
    echo "=== $jar ==="
    strings "$jar" 2>/dev/null | grep -i "L01\|GDO\|seriennum\|serialnum\|chipSerial"
done
