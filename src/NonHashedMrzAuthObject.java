//INFO:  Loaded 3 plugins
//INFO:  JVM info: Arch Linux - 17.0.18 - 17.0.18+8
//INFO:  Scanning classes from Java runtime current
//INFO:  Scanning classes from module java.base@17.0.18
//INFO:  Scanning classes from module java.compiler@17.0.18
//INFO:  Scanning classes from module java.datatransfer@17.0.18
//INFO:  Scanning classes from module java.desktop@17.0.18
//INFO:  Scanning classes from module java.instrument@17.0.18
//INFO:  Scanning classes from module java.logging@17.0.18
//INFO:  Scanning classes from module java.management@17.0.18
//INFO:  Scanning classes from module java.management.rmi@17.0.18
//INFO:  Scanning classes from module java.naming@17.0.18
//INFO:  Scanning classes from module java.net.http@17.0.18
//INFO:  Scanning classes from module java.prefs@17.0.18
//INFO:  Scanning classes from module java.rmi@17.0.18
//INFO:  Scanning classes from module java.scripting@17.0.18
//INFO:  Scanning classes from module java.se@17.0.18
//INFO:  Scanning classes from module java.security.jgss@17.0.18
//INFO:  Scanning classes from module java.security.sasl@17.0.18
//INFO:  Scanning classes from module java.smartcardio@17.0.18
//INFO:  Scanning classes from module java.sql@17.0.18
//INFO:  Scanning classes from module java.sql.rowset@17.0.18
//INFO:  Scanning classes from module java.transaction.xa@17.0.18
//INFO:  Scanning classes from module java.xml@17.0.18
//INFO:  Scanning classes from module java.xml.crypto@17.0.18
//INFO:  Scanning classes from module jdk.accessibility@17.0.18
//INFO:  Scanning classes from module jdk.attach@17.0.18
//INFO:  Scanning classes from module jdk.charsets@17.0.18
//INFO:  Scanning classes from module jdk.compiler@17.0.18
//INFO:  Scanning classes from module jdk.crypto.cryptoki@17.0.18
//INFO:  Scanning classes from module jdk.crypto.ec@17.0.18
//INFO:  Scanning classes from module jdk.dynalink@17.0.18
//INFO:  Scanning classes from module jdk.editpad@17.0.18
//INFO:  Scanning classes from module jdk.hotspot.agent@17.0.18
//INFO:  Scanning classes from module jdk.httpserver@17.0.18
//INFO:  Scanning classes from module jdk.incubator.foreign@17.0.18
//INFO:  Scanning classes from module jdk.incubator.vector@17.0.18
//INFO:  Scanning classes from module jdk.internal.ed@17.0.18
//INFO:  Scanning classes from module jdk.internal.jvmstat@17.0.18
//INFO:  Scanning classes from module jdk.internal.le@17.0.18
//INFO:  Scanning classes from module jdk.internal.opt@17.0.18
//INFO:  Scanning classes from module jdk.internal.vm.ci@17.0.18
//INFO:  Scanning classes from module jdk.internal.vm.compiler@17.0.18
//INFO:  Scanning classes from module jdk.internal.vm.compiler.management@17.0.18
//INFO:  Scanning classes from module jdk.jartool@17.0.18
//INFO:  Scanning classes from module jdk.javadoc@17.0.18
//INFO:  Scanning classes from module jdk.jcmd@17.0.18
//INFO:  Scanning classes from module jdk.jconsole@17.0.18
//INFO:  Scanning classes from module jdk.jdeps@17.0.18
//INFO:  Scanning classes from module jdk.jdi@17.0.18
//INFO:  Scanning classes from module jdk.jdwp.agent@17.0.18
//INFO:  Scanning classes from module jdk.jfr@17.0.18
//INFO:  Scanning classes from module jdk.jlink@17.0.18
//INFO:  Scanning classes from module jdk.jpackage@17.0.18
//INFO:  Scanning classes from module jdk.jshell@17.0.18
//INFO:  Scanning classes from module jdk.jsobject@17.0.18
//INFO:  Scanning classes from module jdk.jstatd@17.0.18
//INFO:  Scanning classes from module jdk.localedata@17.0.18
//INFO:  Scanning classes from module jdk.management@17.0.18
//INFO:  Scanning classes from module jdk.management.agent@17.0.18
//INFO:  Scanning classes from module jdk.management.jfr@17.0.18
//INFO:  Scanning classes from module jdk.naming.dns@17.0.18
//INFO:  Scanning classes from module jdk.naming.rmi@17.0.18
//INFO:  Scanning classes from module jdk.net@17.0.18
//INFO:  Scanning classes from module jdk.nio.mapmode@17.0.18
//INFO:  Scanning classes from module jdk.random@17.0.18
//INFO:  Scanning classes from module jdk.sctp@17.0.18
//INFO:  Scanning classes from module jdk.security.auth@17.0.18
//INFO:  Scanning classes from module jdk.security.jgss@17.0.18
//INFO:  Scanning classes from module jdk.unsupported@17.0.18
//INFO:  Scanning classes from module jdk.unsupported.desktop@17.0.18
//INFO:  Scanning classes from module jdk.xml.dom@17.0.18
//INFO:  Scanning classes from module jdk.zipfs@17.0.18
//INFO:  Scanning classes from file linux/plugins/de.persosim.simulator_1.4.0.20251112/de/persosim/simulator/cardobjects/NonHashedMrzAuthObject.class
//INFO:  Loading Class: de/persosim/simulator/cardobjects/NonHashedMrzAuthObject from file linux/plugins/de.persosim.simulator_1.4.0.20251112/de/persosim/simulator/cardobjects/NonHashedMrzAuthObject.class
//INFO:  Preprocessing class de/persosim/simulator/cardobjects/NonHashedMrzAuthObject
//INFO:  ... done
//INFO:  Decompiling class de/persosim/simulator/cardobjects/NonHashedMrzAuthObject
//INFO:        Loading Class: java/lang/String from module java.base@17.0.18
//INFO:        Loading Class: java/util/Arrays from module java.base@17.0.18
//INFO:  ... done
//==== NonHashedMrzAuthObject.java ====
package de.persosim.simulator.cardobjects;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

public class NonHashedMrzAuthObject extends PasswordAuthObject {
   protected String mrz;
   protected byte[] idPicc;

   public NonHashedMrzAuthObject(AuthObjectIdentifier identifier, String mrz, byte[] idPicc) throws NoSuchAlgorithmException, IOException {
      super(identifier, constructNonHashedMrzPassword(mrz), "MRZ");
      this.mrz = mrz;
      this.idPicc = idPicc;
   }

   public NonHashedMrzAuthObject(AuthObjectIdentifier identifier, String mrz) throws NoSuchAlgorithmException, IOException {
      this(identifier, mrz, MrzAuthObject.extractIdPicc(mrz));
   }

   private static byte[] constructNonHashedMrzPassword(String machineReadableZone) throws NoSuchAlgorithmException, IOException {
      return machineReadableZone.length() == 90 ? MrzAuthObject.getDigestInputTd1(machineReadableZone) : MrzAuthObject.getDigestInputIdl(machineReadableZone);
   }

   public String getMrz() {
      return this.mrz;
   }

   public byte[] getIdPicc() {
      if (this.idPicc == null) {
         this.idPicc = MrzAuthObject.extractIdPicc(this.mrz);
      }

      return Arrays.copyOf(this.idPicc, this.idPicc.length);
   }
}

