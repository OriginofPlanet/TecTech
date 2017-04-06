package com.github.technus.tectech.thing.metaTileEntity.multi;

import com.github.technus.tectech.TecTech;
import com.github.technus.tectech.Util;
import com.github.technus.tectech.CommonValues;
import com.github.technus.tectech.dataFramework.quantumDataPacket;
import com.github.technus.tectech.thing.metaTileEntity.hatch.GT_MetaTileEntity_Hatch_InputData;
import com.github.technus.tectech.thing.metaTileEntity.hatch.GT_MetaTileEntity_Hatch_OutputData;
import com.github.technus.tectech.thing.metaTileEntity.hatch.GT_MetaTileEntity_Hatch_Rack;
import gregtech.api.enums.Textures;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.metatileentity.implementations.GT_MetaTileEntity_Hatch;
import gregtech.api.objects.GT_RenderedTexture;
import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;

import java.util.ArrayList;

import static com.github.technus.tectech.thing.casing.GT_Container_CasingsTT.sBlockCasingsTT;
import static gregtech.api.enums.GT_Values.V;

/**
 * Created by danie_000 on 17.12.2016.
 */
public class GT_MetaTileEntity_EM_computer extends GT_MetaTileEntity_MultiblockBase_EM {
    private final ArrayList<GT_MetaTileEntity_Hatch_Rack> eRacks=new ArrayList<>();

    private static final String[][] front = new String[][]{
            {"  ","  ","+ ","  ",},
    };
    private static final String[][] terminator = new String[][]{
            {"  ","  ","  ","  ",},
    };
    private static final String[][] cap = new String[][]{
            {"01","22","22","01",},
    };
    private static final String[][] slice = new String[][]{
            {"01","!2","!2","01",},
    };
    private static final Block[] blockType = new Block[]{sBlockCasingsTT,sBlockCasingsTT,sBlockCasingsTT};
    private static final byte[] blockMeta = new byte[]{2,0,1};
    private static final String[] addingMethods = new String[]{"addToMachineList","addRackToMachineList"};
    private static final byte[] casingTextures = new byte[]{96,97};
    private static final Block[] blockTypeFallback = new Block[]{sBlockCasingsTT,sBlockCasingsTT};
    private static final byte[] blockMetaFallback = new byte[]{0,1};

    private int maxTemp=0;

    public GT_MetaTileEntity_EM_computer(int aID, String aName, String aNameRegional) {
        super(aID, aName, aNameRegional);
    }

    public GT_MetaTileEntity_EM_computer(String aName) {
        super(aName);
    }

    public IMetaTileEntity newMetaEntity(IGregTechTileEntity aTileEntity) {
        return new GT_MetaTileEntity_EM_computer(this.mName);
    }

    @Override
    public ITexture[] getTexture(IGregTechTileEntity aBaseMetaTileEntity, byte aSide, byte aFacing, byte aColorIndex, boolean aActive, boolean aRedstone) {
        if (aSide == aFacing) {
            return new ITexture[]{Textures.BlockIcons.CASING_BLOCKS[96], new GT_RenderedTexture(aActive ? ScreenON : ScreenOFF)};
        }
        return new ITexture[]{Textures.BlockIcons.CASING_BLOCKS[96]};
    }

    //TODO implement uncertainty,unstability
    @Override
    public boolean EM_checkRecipe(ItemStack itemStack) {
        eAvailableData=0;
        maxTemp=0;
        short thingsActive=0;
        int rackComputation;

        for (GT_MetaTileEntity_Hatch_Rack r : eRacks) {
            if(r.heat>maxTemp)maxTemp=r.heat;
            rackComputation= r.tickComponents(eParamsIn[0]);
            if(rackComputation>0){
                r.getBaseMetaTileEntity().setActive(true);
                eAvailableData+=rackComputation;
                thingsActive+=4;
            }
        }

        for (GT_MetaTileEntity_Hatch_InputData di : eInputData) {
            if(di.q!=null) {//ok for power losses
                thingsActive++;
            }
        }

        if(thingsActive>0){
            thingsActive+=eOutputData.size();
            mEUt = -(int) V[8];
            eAmpereFlow = 1 + ((thingsActive + thingsActive) >> 3);
            mMaxProgresstime = 20;
            mEfficiencyIncrease = 10000;
            return true;
        }
        mMaxProgresstime = 0;
        mEfficiencyIncrease = 0;
        return false;
    }

    @Override
    protected long EM_getAvailableData() {
        return eAvailableData;
    }

    @Override
    public void EM_checkParams() {
        if(eParamsIn[0]<=0)
            eParamsInStatus[0]=PARAM_TOO_LOW;
        else if(eParamsIn[0]<1)
            eParamsInStatus[0]=PARAM_LOW;
        else if(eParamsIn[0]==1)
            eParamsInStatus[0]=PARAM_OK;
        else if(eParamsIn[0]<=2)
            eParamsInStatus[0]=PARAM_HIGH;
        else eParamsInStatus[0]=PARAM_TOO_HIGH;

        eParamsOut[0]=eAvailableData;
        eParamsOut[10]=maxTemp;

        if(maxTemp<-10000)
            eParamsOutStatus[0]=PARAM_TOO_LOW;
        else if(maxTemp<0)
            eParamsOutStatus[0]=PARAM_LOW;
        else if (maxTemp==0)
            eParamsOutStatus[0]=PARAM_OK;
        else if(maxTemp<=5000)
            eParamsOutStatus[0]=PARAM_HIGH;
        else eParamsOutStatus[0]=PARAM_TOO_HIGH;
    }

    @Override
    public void EM_outputFunction() {
        if(eOutputData.size()>0) {
            quantumDataPacket pack = new quantumDataPacket(position, eAvailableData);
            for (GT_MetaTileEntity_Hatch_InputData i : eInputData) {
                if(i.q==null)continue;
                if(i.q.contains(position)){
                    i.q=null;
                    continue;
                }
                pack = pack.unifyPacketWith(i.q);
                i.q = null;
                if (pack == null) return;
            }

            pack.computation /= eOutputData.size();

            for (GT_MetaTileEntity_Hatch_OutputData o : eOutputData) {
                o.q=pack;
            }
        }
    }

    @Override
    public void onRemoval() {
        super.onRemoval();
        for(GT_MetaTileEntity_Hatch_Rack r:eRacks)
            r.getBaseMetaTileEntity().setActive(false);
    }

    @Override
    protected void EM_stopMachine() {
        for(GT_MetaTileEntity_Hatch_Rack r:eRacks)
            r.getBaseMetaTileEntity().setActive(false);
    }

    @Override
    public boolean checkMachine(IGregTechTileEntity iGregTechTileEntity, ItemStack itemStack) {
        eRacks.clear();
        if(!EM_StructureCheckAdvanced(front,blockType,blockMeta,addingMethods,casingTextures,blockTypeFallback,blockMetaFallback,0,2,0))return false;
        if(!EM_StructureCheckAdvanced(cap,blockType,blockMeta,addingMethods,casingTextures,blockTypeFallback,blockMetaFallback,0,2,-1))return false;
        int i;
        for(i=-2;i>-16;i--){
            if(!EM_StructureCheckAdvanced(slice,blockType,blockMeta,addingMethods,casingTextures,blockTypeFallback,blockMetaFallback,0,2,i))break;
        }
        if(!EM_StructureCheckAdvanced(cap,blockType,blockMeta,addingMethods,casingTextures,blockTypeFallback,blockMetaFallback,0,2,++i))return false;
        if(!EM_StructureCheckAdvanced(terminator,blockType,blockMeta,addingMethods,casingTextures,blockTypeFallback,blockMetaFallback,0,2,--i))return false;
        return eUncertainHatches.size() == 1;
    }

    @Override
    protected void EM_extraExplosions() {
        for (MetaTileEntity tTileEntity : eRacks) tTileEntity.getBaseMetaTileEntity().doExplosion(V[9]);
    }

    @Override
    public String[] getDescription() {
        return new String[]{
                CommonValues.tecMark,
                Util.intToString(TecTech.Rnd.nextInt(),8),
                EnumChatFormatting.AQUA.toString() + EnumChatFormatting.BOLD + "You need it to process the number above"
        };
    }

    @Override
    public boolean isFacingValid(byte aFacing) {
        return aFacing>=2;
    }

    //NEW METHOD
    public final boolean addRackToMachineList(IGregTechTileEntity aTileEntity, int aBaseCasingIndex) {
        if (aTileEntity == null) return false;
        IMetaTileEntity aMetaTileEntity = aTileEntity.getMetaTileEntity();
        if (aMetaTileEntity == null) return false;
        if (aMetaTileEntity instanceof GT_MetaTileEntity_Hatch_Rack) {
            ((GT_MetaTileEntity_Hatch) aMetaTileEntity).mMachineBlock = (byte) aBaseCasingIndex;
            return eRacks.add((GT_MetaTileEntity_Hatch_Rack) aMetaTileEntity);
        }
        return false;
    }

    public static void run(){
        try {
            adderMethodMap.put("addRackToMachineList", GT_MetaTileEntity_EM_computer.class.getMethod("addRackToMachineList", IGregTechTileEntity.class, int.class));
        }catch (NoSuchMethodException e){
            if(TecTech.ModConfig.DEBUG_MODE) e.printStackTrace();
        }
    }
}
