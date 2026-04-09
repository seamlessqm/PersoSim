package de.persosim.simulator.protocols.file;

import de.persosim.simulator.apdu.CommandApdu;
import de.persosim.simulator.apdu.ResponseApdu;
import de.persosim.simulator.cardobjects.CardFile;
import de.persosim.simulator.cardobjects.CardObject;
import de.persosim.simulator.cardobjects.CardObjectIdentifier;
import de.persosim.simulator.cardobjects.DedicatedFile;
import de.persosim.simulator.cardobjects.DedicatedFileIdentifier;
import de.persosim.simulator.cardobjects.ElementaryFile;
import de.persosim.simulator.cardobjects.FileIdentifier;
import de.persosim.simulator.cardobjects.ShortFileIdentifier;
import de.persosim.simulator.exception.AccessDeniedException;
import de.persosim.simulator.exception.FileIdentifierIncorrectValueException;
import de.persosim.simulator.exception.ProcessingException;
import de.persosim.simulator.exception.TagNotFoundException;
import de.persosim.simulator.platform.CardStateAccessor;
import de.persosim.simulator.protocols.AbstractProtocolStateMachine;
import de.persosim.simulator.protocols.ProtocolUpdate;
import de.persosim.simulator.secstatus.SecStatusMechanismUpdatePropagation;
import de.persosim.simulator.secstatus.SecStatus.SecContext;
import de.persosim.simulator.tlv.PrimitiveTlvDataObject;
import de.persosim.simulator.tlv.TlvDataObject;
import de.persosim.simulator.tlv.TlvDataObjectContainer;
import de.persosim.simulator.tlv.TlvTag;
import de.persosim.simulator.tlv.TlvValue;
import de.persosim.simulator.tlv.TlvValuePlain;
import de.persosim.simulator.utils.Utils;
import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.Collection;
import org.globaltester.simulator.SimulatorConfiguration;

public abstract class AbstractFileProtocol extends AbstractProtocolStateMachine {
   static final byte P1_MASK_EF_IN_P1_P2 = -128;
   static final byte INS_MASK_ODDINS = 1;
   static final byte P1_MASK_SHORT_FILE_IDENTIFIER = -128;
   static final byte ODDINS_RESPONSE_TAG = 83;
   static final byte ODDINS_COMMAND_TAG = 84;
   static final byte ODDINS_COMMAND_DDO_TAG_73 = 115;
   static final byte ODDINS_COMMAND_DDO_TAG_53 = 83;
   static final short P1P2_MASK_SFI = 31;
   static final byte P1_MASK_SFI = 31;

   public AbstractFileProtocol() {
      super("FM");
   }

   protected void processCommandSelectFile() {
      CommandApdu cmdApdu = this.processingData.getCommandApdu();
      byte p1 = cmdApdu.getP1();
      byte p2 = cmdApdu.getP2();
      if ((p2 & 240) == 0) {
         CardFile file = null;

         try {
            switch (p1) {
               case 0:
                  if ((p2 & 3) == 0) {
                     if (!cmdApdu.getCommandData().isEmpty()
                        && (cmdApdu.getNc() != 2 || !Arrays.equals(cmdApdu.getCommandData().toByteArray(), new byte[]{63, 0}))) {
                        file = getFileForSelection(
                           CurrentFileHandler.getCurrentDedicatedFile(this.cardState),
                           new FileIdentifier(Utils.getShortFromUnsignedByteArray(cmdApdu.getCommandData().toByteArray()))
                        );
                     } else {
                        file = this.handleSelectMf();
                     }
                  } else {
                     ResponseApdu resp = new ResponseApdu((short)27265);
                     this.processingData.updateResponseAPDU(this, "file occurence selector not supported", resp);
                  }
               case 1:
               case 3:
               default:
                  break;
               case 2:
                  byte[] cmdDataRaw = cmdApdu.getCommandData().toByteArray();
                  if (cmdDataRaw == null || cmdDataRaw.length < 1) {
                     throw new FileNotFoundException();
                  }

                  Collection<CardObject> x = CurrentFileHandler.getCurrentDedicatedFile(this.cardState)
                     .findChildren(new CardObjectIdentifier[]{new FileIdentifier(Utils.getShortFromUnsignedByteArray(cmdDataRaw))});
                  if (x.size() != 1) {
                     throw new FileNotFoundException();
                  }

                  file = (CardFile)x.iterator().next();
                  break;
               case 4:
                  file = this.getFileForName(this.cardState.getMasterFile(), new DedicatedFileIdentifier(cmdApdu.getCommandData().toByteArray()));
            }

            if (file != null) {
               if (file instanceof ElementaryFile binaryFile) {
                  binaryFile.getContent();
               }

               this.selectFile(file);
               TlvDataObjectContainer fco = this.getFileControlInformation(file, p2);
               ResponseApdu resp = new ResponseApdu(fco, (short)-28672);
               this.processingData.updateResponseAPDU(this, "file selected successfully", resp);
            }
         } catch (FileNotFoundException var7) {
            ResponseApdu resp = new ResponseApdu((short)27266);
            this.processingData.updateResponseAPDU(this, "file not selected (not found)", resp);
         } catch (NullPointerException var8) {
            ResponseApdu respx = new ResponseApdu((short)26368);
            this.processingData.updateResponseAPDU(this, "file identifier required in command datafield", respx);
         } catch (AccessDeniedException var9) {
            ResponseApdu respxx = new ResponseApdu((short)27010);
            this.processingData.updateResponseAPDU(this, "file selection denied due to unsatisfied security status", respxx);
         }
      } else {
         ResponseApdu resp = new ResponseApdu((short)27265);
         this.processingData.updateResponseAPDU(this, "file occurence selector not supported", resp);
      }

      this.processingData.addUpdatePropagation(this, "FileManagement protocol is not supposed to stay on the stack", new ProtocolUpdate(true));
   }

   protected CardFile handleSelectMf() {
      return this.cardState.getMasterFile();
   }

   private CardFile getFileForName(DedicatedFile df, DedicatedFileIdentifier dfIdentifier) {
      if (dfIdentifier.matches(df)) {
         return df;
      } else {
         for (CardObject curChild : df.getChildren()) {
            if (curChild instanceof DedicatedFile) {
               CardFile candidate = this.getFileForName((DedicatedFile)curChild, dfIdentifier);
               if (candidate != null) {
                  return candidate;
               }
            }
         }

         return null;
      }
   }

   private TlvDataObjectContainer getFileControlInformation(CardFile file, byte p2) {
      switch (p2 & 12) {
         case 0:
            TlvDataObjectContainer result = new TlvDataObjectContainer();
            result.addTlvDataObject(new TlvDataObject[]{file.getFileControlParameterDataObject()});
            result.addTlvDataObject(new TlvDataObject[]{file.getFileManagementDataObject()});
            return result;
         case 4:
            return new TlvDataObjectContainer(new TlvDataObject[]{file.getFileControlParameterDataObject()});
         case 8:
            return new TlvDataObjectContainer(new TlvDataObject[]{file.getFileManagementDataObject()});
         case 12:
            return new TlvDataObjectContainer();
         default:
            return null;
      }
   }

   protected void processCommandEraseBinary() {
      CardFile file;
      try {
         file = (CardFile)getFile(this.processingData.getCommandApdu(), this.cardState, false);
      } catch (FileNotFoundException var7) {
         ResponseApdu resp = new ResponseApdu((short)27266);
         this.processingData.updateResponseAPDU(this, "binary file not found for selection", resp);
         return;
      }

      if (!(file instanceof ElementaryFile ef)) {
         throw new ProcessingException((short)27014, "The used file is not an EF and can note be erased.");
      } else {
         int startingOffset = this.getOffset(this.processingData.getCommandApdu());
         TlvValue apduData = this.processingData.getCommandApdu().getCommandData();

         try {
            if (apduData.getLength() > 0) {
               int endingOffset = Utils.getIntFromUnsignedByteArray(apduData.toByteArray());
               ef.erase(startingOffset, endingOffset);
            } else {
               ef.erase(startingOffset);
            }

            ResponseApdu resp = new ResponseApdu((short)-28672);
            this.processingData.updateResponseAPDU(this, "binary file updated successfully", resp);
            this.processingData.addUpdatePropagation(this, "FileManagement protocol is not supposed to stay on the stack", new ProtocolUpdate(true));
         } catch (AccessDeniedException var6) {
            throw new ProcessingException((short)27010, "The used file can not be erased due to access conditions.");
         }
      }
   }

   protected void processCommandEraseBinaryOdd() {
      CardFile file;
      try {
         file = (CardFile)getFile(this.processingData.getCommandApdu(), this.cardState, true);
      } catch (FileNotFoundException var9) {
         ResponseApdu resp = new ResponseApdu((short)27266);
         this.processingData.updateResponseAPDU(this, "binary file not found for erasing", resp);
         return;
      }

      if (!(file instanceof ElementaryFile ef)) {
         throw new ProcessingException((short)27014, "The used file is not an EF and can not be erased.");
      } else {
         try {
            int startingOffset = -1;
            int endingOffset = -1;

            try {
               startingOffset = Utils.getIntFromUnsignedByteArray(this.getDDO(this.processingData.getCommandApdu(), 0).getValueField());
               endingOffset = Utils.getIntFromUnsignedByteArray(this.getDDO(this.processingData.getCommandApdu(), 1).getValueField());
            } catch (TagNotFoundException var6) {
            }

            if (startingOffset < 0) {
               ef.erase();
            } else if (endingOffset < 0) {
               ef.erase(startingOffset);
            } else {
               ef.erase(startingOffset, endingOffset);
            }

            ResponseApdu resp = new ResponseApdu((short)-28672);
            this.processingData.updateResponseAPDU(this, "binary file erased successfully", resp);
            this.processingData.addUpdatePropagation(this, "FileManagement protocol is not supposed to stay on the stack", new ProtocolUpdate(true));
         } catch (AccessDeniedException var7) {
            throw new ProcessingException((short)27010, "The used file can not be erased due to access conditions.");
         } catch (IllegalArgumentException var8) {
            throw new ProcessingException((short)27012, "The given offsets are invalid.");
         }
      }
   }

    protected void processCommandUpdateBinary() {
	    System.out.println("=== WRITE === offset=" + this.getOffset(this.processingData.getCommandApdu()) + " data(utf8)=" + new String(this.processingData.getCommandApdu().getCommandData().toByteArray(), java.nio.charset.StandardCharsets.UTF_8));
      System.out.println("returned Fake write success");
	    ResponseApdu resp = new ResponseApdu((short)-28672);
	    this.processingData.updateResponseAPDU(this, "Returned Fake write success", resp);
	    this.processingData.addUpdatePropagation(this, "FileManagement protocol is not supposed to stay on the stack", new ProtocolUpdate(true));
    }

   //protected void processCommandUpdateBinary() {
   //   boolean isOddInstruction = (this.processingData.getCommandApdu().getIns() & 1) == 1;
   //
   //   CardFile file;
   //   try {
   //      file = (CardFile)getFile(this.processingData.getCommandApdu(), this.cardState, isOddInstruction);
   //   } catch (FileNotFoundException var9) {
   //      ResponseApdu resp = new ResponseApdu((short)27266);
   //      this.processingData.updateResponseAPDU(this, "binary file not found for selection", resp);
   //      return;
   //   }
   //
   //   int updateOffset = this.getOffset(this.processingData.getCommandApdu());
   //   byte[] updateData = null;
   //
   //   try {
   //      if (isOddInstruction) {
   //         updateData = this.getDDO(this.processingData.getCommandApdu(), 1).getValueField();
   //      } else {
   //         updateData = this.processingData.getCommandApdu().getCommandData().toByteArray();
   //      }
   //
   //      if (file instanceof ElementaryFile) {
   //         try {
   //            ((ElementaryFile)file).update(updateOffset, updateData);
   //            this.selectFile(file);
   //            ResponseApdu resp = new ResponseApdu((short)-28672);
   //            this.processingData.updateResponseAPDU(this, "binary file updated successfully", resp);
   //         } catch (AccessDeniedException var7) {
   //            ResponseApdu respx = new ResponseApdu((short)27013);
   //            this.processingData.updateResponseAPDU(this, var7.getMessage(), respx);
   //         }
   //      } else {
   //         ResponseApdu resp = new ResponseApdu((short)27014);
   //         this.processingData.updateResponseAPDU(this, "no elementary file", resp);
   //      }
   //   } catch (TagNotFoundException var8) {
   //      ResponseApdu resp = new ResponseApdu((short)27272);
   //      this.processingData.updateResponseAPDU(this, var8.getMessage(), resp);
   //   }
   //
   //   this.processingData.addUpdatePropagation(this, "FileManagement protocol is not supposed to stay on the stack", new ProtocolUpdate(true));
   //}

   private int getOffset(CommandApdu apdu) {
      boolean isOddInstruction = (apdu.getIns() & 1) == 1;
      return isOddInstruction ? this.getOffset(apdu.getCommandDataObjectContainer()) : this.getOffset(apdu.getP1(), apdu.getP2());
   }

   private TlvDataObject getDDO(CommandApdu apdu, int ddoNumber) throws TagNotFoundException {
      TlvDataObjectContainer ddoEncapsulation = apdu.getCommandDataObjectContainer();
      if (ddoEncapsulation.getNoOfElements() <= ddoNumber) {
         throw new TagNotFoundException("DDO encapsulation object does not contain enough DDOs.");
      } else {
         TlvDataObject candidate = (TlvDataObject)ddoEncapsulation.getTlvObjects().get(ddoNumber);
         if (!candidate.getTlvTag().equals(new TlvTag((byte)84))
            && !candidate.getTlvTag().equals(new TlvTag((byte)115))
            && !candidate.getTlvTag().equals(new TlvTag((byte)83))) {
            throw new TagNotFoundException("DDO at index " + ddoNumber + " does not have tag 84");
         } else {
            return candidate;
         }
      }
   }

   private int getOffset(byte p1, byte p2) {
      boolean isShortFileIdentifier = (p1 & -128) == -128;
      return isShortFileIdentifier ? p2 : Utils.concatenate(p1, p2);
   }

   private int getOffset(TlvDataObjectContainer tlv) {
      TlvDataObject offset = tlv.getTlvDataObject(new TlvTag((byte)84));
      return Utils.getIntFromUnsignedByteArray(offset.getValueField());
   }

   private static CardObject getFileOddInstruction(CommandApdu apdu, CardStateAccessor cardState) throws FileNotFoundException {
      if ((apdu.getP1P2() | 31) == 31 && apdu.getP1P2() != 0 && apdu.getP1P2() != 31) {
         return getFileForSelection(CurrentFileHandler.getCurrentDedicatedFile(cardState), new ShortFileIdentifier(apdu.getP1P2()));
      } else {
         return apdu.getP1P2() == 0
            ? CurrentFileHandler.getCurrentFile(cardState)
            : getFileForSelection(CurrentFileHandler.getCurrentDedicatedFile(cardState), new FileIdentifier(apdu.getP1P2()));
      }
   }

   public static CardFile getFileForSelection(DedicatedFile currentDf, CardObjectIdentifier identifier) throws FileNotFoundException {
      for (CardObject curChild : currentDf.getChildren()) {
         if (identifier.matches(curChild) && curChild instanceof CardFile) {
            return (CardFile)curChild;
         }
      }

      if (currentDf.getParent() instanceof DedicatedFile) {
         DedicatedFile parentDf = (DedicatedFile)currentDf.getParent();
         if (identifier.matches(parentDf)) {
            return parentDf;
         }

         for (CardObject curChildx : parentDf.getChildren()) {
            if (identifier.matches(curChildx) && curChildx instanceof CardFile) {
               return (CardFile)curChildx;
            }
         }
      }

      throw new FileNotFoundException();
   }

   private static CardObject getFileEvenInstruction(CommandApdu apdu, CardStateAccessor cardState) throws FileNotFoundException {
      if ((apdu.getP1() & -128) == -128) {
         int shortFileIdentifier = apdu.getP1() & 31;
         if (1 <= shortFileIdentifier && 30 >= shortFileIdentifier) {
            return getFileForSelection(CurrentFileHandler.getCurrentDedicatedFile(cardState), new ShortFileIdentifier(shortFileIdentifier));
         } else {
            throw new FileIdentifierIncorrectValueException();
         }
      } else {
         return CurrentFileHandler.getCurrentFile(cardState);
      }
   }

   protected static CardObject getFile(CommandApdu apdu, CardStateAccessor cardState, boolean isOddInstruction) throws FileNotFoundException {
      return isOddInstruction ? getFileOddInstruction(apdu, cardState) : getFileEvenInstruction(apdu, cardState);
   }

   protected void processCommandReadBinary() {
      byte ins = this.processingData.getCommandApdu().getIns();
      int ne = this.processingData.getCommandApdu().getNe();
      int maxSize = SimulatorConfiguration.getMaxPayloadSize();
      if (ne > maxSize) {
         ne = maxSize;
      }

      boolean isOddInstruction = (ins & 1) == 1;
      boolean zeroEncoded = this.processingData.getCommandApdu().isNeZeroEncoded();
      int offset = this.getOffset(this.processingData.getCommandApdu());
      CardObject file = null;

      try {
         file = getFile(this.processingData.getCommandApdu(), this.cardState, isOddInstruction);
      } catch (FileNotFoundException var15) {
         ResponseApdu resp = new ResponseApdu((short)27266);
         this.processingData.updateResponseAPDU(this, "binary file not found for selection", resp);
      }

      if (file != null) {
         if (file instanceof ElementaryFile binaryFile) {
            try {
               byte[] rawFileContents = binaryFile.getContent();
               if (offset < rawFileContents.length) {
                  int bytesToBeRead = Math.min(ne, rawFileContents.length - offset);
                  byte[] data = Arrays.copyOfRange(rawFileContents, offset, offset + bytesToBeRead);
                  TlvValue toSend = null;
                  if (isOddInstruction) {
                     int includedDataLegnth = data.length;
                     toSend = new TlvDataObjectContainer(
                        new TlvDataObject[]{new PrimitiveTlvDataObject(new TlvTag((byte)83), Arrays.copyOf(data, includedDataLegnth))}
                     );

                     while (toSend.getLength() > ne) {
                        toSend = new TlvDataObjectContainer(
                           new TlvDataObject[]{new PrimitiveTlvDataObject(new TlvTag((byte)83), Arrays.copyOf(data, --includedDataLegnth))}
                        );
                     }
                  } else {
                     toSend = new TlvValuePlain(data);
                  }

                  boolean shortRead = !zeroEncoded && toSend.getLength() < ne;
                  this.selectFile((CardFile)file);
                  ResponseApdu resp = new ResponseApdu(toSend, (short)(shortRead ? 25218 : -28672));
                  this.processingData.updateResponseAPDU(this, "binary file read successfully", resp);
               } else {
                  ResponseApdu resp = new ResponseApdu((short)27392);
                  this.processingData.updateResponseAPDU(this, "offset behind end of file", resp);
               }
            } catch (AccessDeniedException var16) {
               ResponseApdu resp = new ResponseApdu((short)27010);
               this.processingData.updateResponseAPDU(this, "binary file read access denied", resp);
            }
         } else {
            ResponseApdu resp = new ResponseApdu((short)27014);
            this.processingData.updateResponseAPDU(this, "not an elemental file", resp);
         }
      }

      this.processingData.addUpdatePropagation(this, "FileManagement protocol is not supposed to stay on the stack", new ProtocolUpdate(true));
   }

   private void selectFile(CardFile file) {
      this.processingData
         .addUpdatePropagation(this, "select file", new SecStatusMechanismUpdatePropagation(SecContext.GLOBAL, new CurrentFileSecMechanism(file)));
   }
}

