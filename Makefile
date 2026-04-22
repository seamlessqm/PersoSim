compile:
	"C:/path/to/java17/bin/javac" -cp "plugins/*;plugins/de.persosim.simulator_1.4.0.20251112" AbstractFileProtocol.java -d output/

ensure-pcsc:
	systemctl start pcscd.service

AbstractFileProtocol-path:
	@echo plugins/de.persosim.simulator_1.4.0.20251112/de/persosim/simulator/protocols/file

AbstractFileProtocol:
	javac -cp "plugins/*;plugins/de.persosim.simulator_1.4.0.20251112" AbstractFileProtocol.java -d output/

backup-abstract-file-protocol:
	cp linux/plugins/de.persosim.simulator_1.4.0.20251112/de/persosim/simulator/protocols/file/AbstractFileProtocol.class linux/plugins/de.persosim.simulator_1.4.0.20251112/de/persosim/simulator/protocols/file/AbstractFileProtocol.bak

deploy-abstract-file-protocol:
	cp output/de/persosim/simulator/protocols/file/AbstractFileProtocol.class linux/plugins/de.persosim.simulator_1.4.0.20251112/de/persosim/simulator/protocols/file/AbstractFileProtocol.class 

simulate-write-request:
	opensc-tool -r 0 --send-apdu 00D6000005AABBCCDDEE

patch-application:
	cd patches && jar uf /home/joseph/work/PersoSim/linux/plugins/de.persosim.rcp_1.4.0.20251112.jar Application.e4xmi

patch-fragment:
	cd patches && jar uf /home/joseph/work/PersoSim/linux/plugins/de.persosim.simulator.ui_1.4.0.20251112.jar fragment.e4xmi

build:
	rm -rf persosim@sqm-$(VERSION)-linux
	cp -r linux persosim@sqm-$(VERSION)-linux
	zip -r persosim@sqm-$(VERSION)-linux.zip persosim@sqm-$(VERSION)-linux/
	rm -rf persosim@sqm-$(VERSION)-linux
