/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  de.persosim.simulator.exception.AccessDeniedException
 *  de.persosim.simulator.exception.FileIdentifierIncorrectValueException
 *  de.persosim.simulator.exception.ProcessingException
 *  de.persosim.simulator.exception.TagNotFoundException
 *  de.persosim.simulator.utils.Utils
 *  org.globaltester.simulator.SimulatorConfiguration
 */
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
import de.persosim.simulator.protocols.file.CurrentFileHandler;
import de.persosim.simulator.protocols.file.CurrentFileSecMechanism;
import de.persosim.simulator.secstatus.SecStatus;
import de.persosim.simulator.secstatus.SecStatusMechanismUpdatePropagation;
import de.persosim.simulator.tlv.PrimitiveTlvDataObject;
import de.persosim.simulator.tlv.TlvDataObject;
import de.persosim.simulator.tlv.TlvDataObjectContainer;
import de.persosim.simulator.tlv.TlvTag;
import de.persosim.simulator.tlv.TlvValue;
import de.persosim.simulator.tlv.TlvValuePlain;
import de.persosim.simulator.utils.Utils;
import java.io.FileNotFoundException;
import java.util.Arrays;
import org.globaltester.simulator.SimulatorConfiguration;

public abstract class AbstractFileProtocol
extends AbstractProtocolStateMachine {
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
        ResponseApdu resp = null;
        CardFile file = null;

        // Check if file occurrence selector is not supported
        if ((p2 & 0xF0) != 0) {
            resp = new ResponseApdu((short)27265);
            this.processingData.updateResponseAPDU(this, "file occurence selector not supported", resp);
        } else {
            try {
                switch (p1) {
                    case 0: {
                        // Select MF or by file identifier
                        if ((p2 & 3) == 0) {
                            if (cmdApdu.getCommandData().isEmpty()) {
                                file = this.handleSelectMf();
                            } else if (cmdApdu.getNc() == 2) {
                                byte[] expectedPrefix = new byte[2];
                                expectedPrefix[0] = 63;
                                if (Arrays.equals(cmdApdu.getCommandData().toByteArray(), expectedPrefix)) {
                                    file = this.handleSelectMf();
                                } else {
                                    file = AbstractFileProtocol.getFileForSelection(
                                        CurrentFileHandler.getCurrentDedicatedFile(this.cardState),
                                        new FileIdentifier(Utils.getShortFromUnsignedByteArray(cmdApdu.getCommandData().toByteArray()))
                                    );
                                }
                            } else {
                                file = AbstractFileProtocol.getFileForSelection(
                                    CurrentFileHandler.getCurrentDedicatedFile(this.cardState),
                                    new FileIdentifier(Utils.getShortFromUnsignedByteArray(cmdApdu.getCommandData().toByteArray()))
                                );
                            }
                        } else {
                            resp = new ResponseApdu((short)27265);
                            this.processingData.updateResponseAPDU(this, "file occurence selector not supported", resp);
                        }
                        break;
                    }
                    case 2: {
                        // Select by child DF name
                        byte[] cmdDataRaw = cmdApdu.getCommandData().toByteArray();
                        if (cmdDataRaw == null || cmdDataRaw.length < 1) {
                            throw new FileNotFoundException();
                        }
                        java.util.Collection<CardObject> children = CurrentFileHandler.getCurrentDedicatedFile(this.cardState)
                            .findChildren(new CardObjectIdentifier[]{new FileIdentifier(Utils.getShortFromUnsignedByteArray(cmdDataRaw))});
                        if (children.size() != 1) {
                            throw new FileNotFoundException();
                        }
                        file = (CardFile) children.iterator().next();
                        break;
                    }
                    case 4: {
                        // Select by name
                        file = this.getFileForName(this.cardState.getMasterFile(),
                            new DedicatedFileIdentifier(cmdApdu.getCommandData().toByteArray()));
                        break;
                    }
                }

                if (file != null) {
                    if (file instanceof ElementaryFile) {
                        ElementaryFile binaryFile = (ElementaryFile) file;
                        binaryFile.getContent();
                    }
                    this.selectFile(file);
                    TlvDataObjectContainer fco = this.getFileControlInformation(file, p2);
                    resp = new ResponseApdu(fco, (short)-28672);
                    this.processingData.updateResponseAPDU(this, "file selected successfully", resp);
                }
            } catch (FileNotFoundException e) {
                resp = new ResponseApdu((short)27266);
                this.processingData.updateResponseAPDU(this, "file not selected (not found)", resp);
            } catch (NullPointerException e) {
                resp = new ResponseApdu((short)26368);
                this.processingData.updateResponseAPDU(this, "file identifier required in command datafield", resp);
            } catch (AccessDeniedException e) {
                resp = new ResponseApdu((short)27010);
                this.processingData.updateResponseAPDU(this, "file selection denied due to unsatisfied security status", resp);
            }
        }

        this.processingData.addUpdatePropagation(this, "FileManagement protocol is not supposed to stay on the stack", new ProtocolUpdate(true));
    }

    protected CardFile handleSelectMf() {
        return this.cardState.getMasterFile();
    }

    private CardFile getFileForName(DedicatedFile df, DedicatedFileIdentifier dfIdentifier) {
        if (dfIdentifier.matches(df)) {
            return df;
        }
        for (CardObject curChild : df.getChildren()) {
            CardFile candidate;
            if (!(curChild instanceof DedicatedFile) || (candidate = this.getFileForName((DedicatedFile)curChild, dfIdentifier)) == null) continue;
            return candidate;
        }
        return null;
    }

    private TlvDataObjectContainer getFileControlInformation(CardFile file, byte p2) {
        switch (p2 & 0xC) {
            case 0: {
                TlvDataObjectContainer result = new TlvDataObjectContainer();
                result.addTlvDataObject(file.getFileControlParameterDataObject());
                result.addTlvDataObject(file.getFileManagementDataObject());
                return result;
            }
            case 4: {
                return new TlvDataObjectContainer(file.getFileControlParameterDataObject());
            }
            case 8: {
                return new TlvDataObjectContainer(file.getFileManagementDataObject());
            }
            case 12: {
                return new TlvDataObjectContainer();
            }
        }
        return null;
    }

    protected void processCommandEraseBinary() {
        CardFile file;
        try {
            file = (CardFile)AbstractFileProtocol.getFile(this.processingData.getCommandApdu(), this.cardState, false);
        }
        catch (FileNotFoundException e) {
            ResponseApdu resp = new ResponseApdu((short)27266);
            this.processingData.updateResponseAPDU(this, "binary file not found for selection", resp);
            return;
        }
        if (!(file instanceof ElementaryFile)) {
            throw new ProcessingException((short)27014, "The used file is not an EF and can note be erased.");
        }
        ElementaryFile ef = (ElementaryFile)file;
        int startingOffset = this.getOffset(this.processingData.getCommandApdu());
        TlvValue apduData = this.processingData.getCommandApdu().getCommandData();
        try {
            if (apduData.getLength() > 0) {
                int endingOffset = Utils.getIntFromUnsignedByteArray((byte[])apduData.toByteArray());
                ef.erase(startingOffset, endingOffset);
            } else {
                ef.erase(startingOffset);
            }
            ResponseApdu resp = new ResponseApdu((short)-28672);
            this.processingData.updateResponseAPDU(this, "binary file updated successfully", resp);
            this.processingData.addUpdatePropagation(this, "FileManagement protocol is not supposed to stay on the stack", new ProtocolUpdate(true));
        }
        catch (AccessDeniedException e) {
            throw new ProcessingException((short)27010, "The used file can not be erased due to access conditions.");
        }
    }

    protected void processCommandEraseBinaryOdd() {
        CardFile file;
        try {
            file = (CardFile)AbstractFileProtocol.getFile(this.processingData.getCommandApdu(), this.cardState, true);
        }
        catch (FileNotFoundException e) {
            ResponseApdu resp = new ResponseApdu((short)27266);
            this.processingData.updateResponseAPDU(this, "binary file not found for erasing", resp);
            return;
        }
        if (!(file instanceof ElementaryFile)) {
            throw new ProcessingException((short)27014, "The used file is not an EF and can not be erased.");
        }
        ElementaryFile ef = (ElementaryFile)file;
        try {
            int startingOffset = -1;
            int endingOffset = -1;
            try {
                startingOffset = Utils.getIntFromUnsignedByteArray((byte[])this.getDDO(this.processingData.getCommandApdu(), 0).getValueField());
                endingOffset = Utils.getIntFromUnsignedByteArray((byte[])this.getDDO(this.processingData.getCommandApdu(), 1).getValueField());
            }
            catch (TagNotFoundException tagNotFoundException) {
                // empty catch block
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
        }
        catch (AccessDeniedException e) {
            throw new ProcessingException((short)27010, "The used file can not be erased due to access conditions.");
        }
        catch (IllegalArgumentException e) {
            throw new ProcessingException((short)27012, "The given offsets are invalid.");
        }
    }

    protected void processCommandUpdateBinary() {
	    System.out.println("=== WRITE === offset=" + this.getOffset(this.processingData.getCommandApdu()) + " data(utf8)=" + new String(this.processingData.getCommandApdu().getCommandData().toByteArray(), java.nio.charset.StandardCharsets.UTF_8));
	    ResponseApdu resp = new ResponseApdu((short)-28672);
	    this.processingData.updateResponseAPDU(this, "Returned Fake write success", resp);
	    this.processingData.addUpdatePropagation(this, "FileManagement protocol is not supposed to stay on the stack", new ProtocolUpdate(true));
    }

    // protected void processCommandUpdateBinary() {
    //     CardFile file;
    //     boolean isOddInstruction = (this.processingData.getCommandApdu().getIns() & 1) == 1;
    //     try {
    //         file = (CardFile)AbstractFileProtocol.getFile(this.processingData.getCommandApdu(), this.cardState, isOddInstruction);
    //     }
    //     catch (FileNotFoundException e) {
    //         ResponseApdu resp = new ResponseApdu(27266);
    //         this.processingData.updateResponseAPDU(this, "binary file not found for selection", resp);
    //         return;
    //     }
    //     int updateOffset = this.getOffset(this.processingData.getCommandApdu());
    //     byte[] updateData = null;
    //     try {
    //         updateData = isOddInstruction ? this.getDDO(this.processingData.getCommandApdu(), 1).getValueField() : this.processingData.getCommandApdu().getCommandData().toByteArray();
    //         if (file instanceof ElementaryFile) {
    //             try {
    //                 ((ElementaryFile)file).update(updateOffset, updateData);
    //                 this.selectFile(file);
    //                 ResponseApdu resp = new ResponseApdu(-28672);
    //                 this.processingData.updateResponseAPDU(this, "binary file updated successfully", resp);
    //             }
    //             catch (AccessDeniedException e) {
    //                 ResponseApdu resp = new ResponseApdu(27013);
    //                 this.processingData.updateResponseAPDU(this, e.getMessage(), resp);
    //             }
    //         } else {
    //             ResponseApdu resp = new ResponseApdu(27014);
    //             this.processingData.updateResponseAPDU(this, "no elementary file", resp);
    //         }
    //     }
    //     catch (TagNotFoundException e) {
    //         ResponseApdu resp = new ResponseApdu(27272);
    //         this.processingData.updateResponseAPDU(this, e.getMessage(), resp);
    //     }
    //     this.processingData.addUpdatePropagation(this, "FileManagement protocol is not supposed to stay on the stack", new ProtocolUpdate(true));
    // }

    private int getOffset(CommandApdu apdu) {
        boolean isOddInstruction;
        boolean bl = isOddInstruction = (apdu.getIns() & 1) == 1;
        if (isOddInstruction) {
            return this.getOffset(apdu.getCommandDataObjectContainer());
        }
        return this.getOffset(apdu.getP1(), apdu.getP2());
    }

    private TlvDataObject getDDO(CommandApdu apdu, int ddoNumber) throws TagNotFoundException {
        TlvDataObjectContainer ddoEncapsulation = apdu.getCommandDataObjectContainer();
        if (ddoEncapsulation.getNoOfElements() <= ddoNumber) {
            throw new TagNotFoundException("DDO encapsulation object does not contain enough DDOs.");
        }
        TlvDataObject candidate = ddoEncapsulation.getTlvObjects().get(ddoNumber);
        if (candidate.getTlvTag().equals(new TlvTag((byte)84)) || candidate.getTlvTag().equals(new TlvTag((byte)115)) || candidate.getTlvTag().equals(new TlvTag((byte)83))) {
            return candidate;
        }
        throw new TagNotFoundException("DDO at index " + ddoNumber + " does not have tag 84");
    }

    private int getOffset(byte p1, byte p2) {
        boolean isShortFileIdentifier;
        boolean bl = isShortFileIdentifier = (p1 & 0xFFFFFF80) == -128;
        if (isShortFileIdentifier) {
            return p2;
        }
        return Utils.concatenate((byte)p1, (byte)p2);
    }

    private int getOffset(TlvDataObjectContainer tlv) {
        TlvDataObject offset = tlv.getTlvDataObject(new TlvTag((byte)84));
        return Utils.getIntFromUnsignedByteArray((byte[])offset.getValueField());
    }

    private static CardObject getFileOddInstruction(CommandApdu apdu, CardStateAccessor cardState) throws FileNotFoundException {
        if ((apdu.getP1P2() | 0x1F) == 31 && apdu.getP1P2() != 0 && apdu.getP1P2() != 31) {
            return AbstractFileProtocol.getFileForSelection(CurrentFileHandler.getCurrentDedicatedFile(cardState), new ShortFileIdentifier(apdu.getP1P2()));
        }
        if (apdu.getP1P2() == 0) {
            return CurrentFileHandler.getCurrentFile(cardState);
        }
        return AbstractFileProtocol.getFileForSelection(CurrentFileHandler.getCurrentDedicatedFile(cardState), new FileIdentifier(apdu.getP1P2()));
    }

    public static CardFile getFileForSelection(DedicatedFile currentDf, CardObjectIdentifier identifier) throws FileNotFoundException {
        for (CardObject curChild : currentDf.getChildren()) {
            if (!identifier.matches(curChild) || !(curChild instanceof CardFile)) continue;
            return (CardFile)curChild;
        }
        if (currentDf.getParent() instanceof DedicatedFile) {
            DedicatedFile parentDf = (DedicatedFile)currentDf.getParent();
            if (identifier.matches(parentDf)) {
                return parentDf;
            }
            for (CardObject curChild : parentDf.getChildren()) {
                if (!identifier.matches(curChild) || !(curChild instanceof CardFile)) continue;
                return (CardFile)curChild;
            }
        }
        throw new FileNotFoundException();
    }

    private static CardObject getFileEvenInstruction(CommandApdu apdu, CardStateAccessor cardState) throws FileNotFoundException {
        if ((apdu.getP1() & 0xFFFFFF80) == -128) {
            int shortFileIdentifier = apdu.getP1() & 0x1F;
            if (1 > shortFileIdentifier || 30 < shortFileIdentifier) {
                throw new FileIdentifierIncorrectValueException();
            }
            return AbstractFileProtocol.getFileForSelection(CurrentFileHandler.getCurrentDedicatedFile(cardState), new ShortFileIdentifier(shortFileIdentifier));
        }
        return CurrentFileHandler.getCurrentFile(cardState);
    }

    protected static CardObject getFile(CommandApdu apdu, CardStateAccessor cardState, boolean isOddInstruction) throws FileNotFoundException {
        if (isOddInstruction) {
            return AbstractFileProtocol.getFileOddInstruction(apdu, cardState);
        }
        return AbstractFileProtocol.getFileEvenInstruction(apdu, cardState);
    }

    protected void processCommandReadBinary() {
        block12: {
            ResponseApdu resp;
            int maxSize;
            byte ins = this.processingData.getCommandApdu().getIns();
            int ne = this.processingData.getCommandApdu().getNe();
            if (ne > (maxSize = SimulatorConfiguration.getMaxPayloadSize())) {
                ne = maxSize;
            }
            boolean isOddInstruction = (ins & 1) == 1;
            boolean zeroEncoded = this.processingData.getCommandApdu().isNeZeroEncoded();
            int offset = this.getOffset(this.processingData.getCommandApdu());
            CardObject file = null;
            try {
                file = AbstractFileProtocol.getFile(this.processingData.getCommandApdu(), this.cardState, isOddInstruction);
            }
            catch (FileNotFoundException e) {
                resp = new ResponseApdu((short)27266);
                this.processingData.updateResponseAPDU(this, "binary file not found for selection", resp);
            }
            if (file != null) {
                if (file instanceof ElementaryFile) {
                    try {
                        ElementaryFile binaryFile = (ElementaryFile)file;
                        byte[] rawFileContents = binaryFile.getContent();
                        if (offset < rawFileContents.length) {
                            int bytesToBeRead = Math.min(ne, rawFileContents.length - offset);
                            byte[] data = Arrays.copyOfRange(rawFileContents, offset, offset + bytesToBeRead);
                            TlvValue toSend = null;
                            if (isOddInstruction) {
                                int includedDataLegnth = data.length;
                                toSend = new TlvDataObjectContainer(new PrimitiveTlvDataObject(new TlvTag((byte)83), Arrays.copyOf(data, includedDataLegnth)));
                                while (toSend.getLength() > ne) {
                                    toSend = new TlvDataObjectContainer(new PrimitiveTlvDataObject(new TlvTag((byte)83), Arrays.copyOf(data, --includedDataLegnth)));
                                }
                            } else {
                                toSend = new TlvValuePlain(data);
                            }
                            boolean shortRead = !zeroEncoded && toSend.getLength() < ne;
                            this.selectFile((CardFile)file);
                            ResponseApdu resp2 = new ResponseApdu(toSend, shortRead ? (short)25218 : (short)-28672);
                            this.processingData.updateResponseAPDU(this, "binary file read successfully", resp2);
                            break block12;
                        }
                        ResponseApdu resp3 = new ResponseApdu((short)27392);
                        this.processingData.updateResponseAPDU(this, "offset behind end of file", resp3);
                    }
                    catch (AccessDeniedException e) {
                        resp = new ResponseApdu((short)27010);
                        this.processingData.updateResponseAPDU(this, "binary file read access denied", resp);
                    }
                } else {
                    ResponseApdu resp4 = new ResponseApdu((short)27014);
                    this.processingData.updateResponseAPDU(this, "not an elemental file", resp4);
                }
            }
        }
        this.processingData.addUpdatePropagation(this, "FileManagement protocol is not supposed to stay on the stack", new ProtocolUpdate(true));
    }

    private void selectFile(CardFile file) {
        this.processingData.addUpdatePropagation(this, "select file", new SecStatusMechanismUpdatePropagation(SecStatus.SecContext.GLOBAL, new CurrentFileSecMechanism(file)));
    }
}
