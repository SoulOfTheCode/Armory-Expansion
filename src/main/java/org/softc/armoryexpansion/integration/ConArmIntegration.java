package org.softc.armoryexpansion.integration;

import c4.conarm.ConstructsArmory;
import c4.conarm.lib.materials.CoreMaterialStats;
import c4.conarm.lib.materials.PlatesMaterialStats;
import c4.conarm.lib.materials.TrimMaterialStats;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import org.softc.armoryexpansion.ArmoryExpansion;
import org.softc.armoryexpansion.integration.aelib.AbstractIntegration;
import org.softc.armoryexpansion.integration.plugins.tinkers_construct.ITiCMaterial;
import org.softc.armoryexpansion.integration.plugins.tinkers_construct.TiCMaterial;
import slimeknights.tconstruct.library.TinkerRegistry;
import slimeknights.tconstruct.library.materials.ExtraMaterialStats;
import slimeknights.tconstruct.library.materials.HandleMaterialStats;
import slimeknights.tconstruct.library.materials.HeadMaterialStats;
import slimeknights.tconstruct.library.materials.Material;
import slimeknights.tconstruct.tools.TinkerMaterials;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import static c4.conarm.lib.materials.ArmorMaterialType.*;
import static org.softc.armoryexpansion.util.Math.clamp;
import static slimeknights.tconstruct.library.materials.MaterialTypes.*;

@Mod(
        modid = ConArmIntegration.MODID,
        name = ConArmIntegration.NAME,
        version = ArmoryExpansion.VERSION,
        dependencies = ConArmIntegration.DEPENDENCIES
)
@Mod.EventBusSubscriber
public class ConArmIntegration extends AbstractIntegration {
    static final String MODID = ArmoryExpansion.MODID + "-" + ConstructsArmory.MODID;
    static final String NAME = ArmoryExpansion.NAME + " - " + ConstructsArmory.MODNAME;
    static final String DEPENDENCIES =
                    "required-after:" + ArmoryExpansion.MODID + "; " +
                    "after:*";

    private List<TiCMaterial> jsonMaterials = new LinkedList<>();

    private void loadMaterialsFromOtherIntegrations(FMLPreInitializationEvent event){

        GsonBuilder builder = new GsonBuilder().setPrettyPrinting().setLenient();
        Gson gson = builder.create();
        String integrationJsonsLocation = event.getModConfigurationDirectory().getPath() + "/" + ArmoryExpansion.MODID + "/";
        File integrationJsonsFolder = new File(integrationJsonsLocation);

        for (File json : Objects.requireNonNull(integrationJsonsFolder.listFiles((dir, name) -> name.contains(".json")))){
            try {
                Collections.addAll(jsonMaterials, gson.fromJson(new FileReader(json), TiCMaterial[].class));
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        this.modid = ConstructsArmory.MODID;
        this.loadMaterialsFromOtherIntegrations(event);
        super.preInit(event);
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        super.init(event);
    }

    @Override
    protected void loadMaterialsFromSource() {
        final float STAT_MULT = 1.25f;
        final int DURA_MIN = 1;
        final int DURA_MAX = 120;
        final int DEF_MIN = 0;
        final int DEF_MAX = 50;
        final int TOUGH_MIN = DEF_MIN / 10;
        final int TOUGH_MAX = DEF_MAX / 10;

        final HeadMaterialStats ironHead = TinkerMaterials.iron.getStats(HEAD);
        final CoreMaterialStats ironCore = TinkerMaterials.iron.getStats(CORE);
        final HandleMaterialStats ironHandle = TinkerMaterials.iron.getStats(HANDLE);
        final PlatesMaterialStats ironPlates = TinkerMaterials.iron.getStats(PLATES);
        final ExtraMaterialStats ironExtra = TinkerMaterials.iron.getStats(EXTRA);
        final TrimMaterialStats ironTrim = TinkerMaterials.iron.getStats(TRIM);

        for (Material material:TinkerRegistry.getAllMaterials())
        {
            final boolean core = !material.hasStats(CORE) && material.hasStats(HEAD);
            final boolean plates = !material.hasStats(PLATES) && material.hasStats(HANDLE);
            final boolean trim = !material.hasStats(TRIM) && material.hasStats(EXTRA);
            final boolean mat = core || plates || trim;

            if (mat) {
                final HeadMaterialStats materialHead = material.getStats(HEAD);
                final HandleMaterialStats materialHandle = material.getStats(HANDLE);
                final ExtraMaterialStats materialExtra = material.getStats(EXTRA);

                int durability = materialHead != null ? (int)clamp(ironCore.durability * materialHead.durability / ironHead.durability / STAT_MULT, DURA_MIN, DURA_MAX): 0;
                float defense = materialHead != null ? clamp(1.5f * ironCore.defense * materialHead.attack / ironHead.attack  / STAT_MULT, DEF_MIN,DEF_MAX) : 0;
                float toughness = materialHandle != null ? clamp(3 * ironPlates.toughness * materialHandle.durability / ironHandle.durability / STAT_MULT, TOUGH_MIN, TOUGH_MAX) : 0;
                float extra = materialExtra != null ? 2 * ironTrim.extraDurability * materialExtra.extraDurability / ironExtra.extraDurability / STAT_MULT : 0;

                ITiCMaterial m = new TiCMaterial(material.identifier, null, material.materialTextColor)
                        .setArmorMaterial(true)
                        .setDurability(durability)
                        .setMagicAffinity(extra)
                        .setDefense(defense)
                        .setToughness(toughness);
                //noinspection SuspiciousMethodCalls
                if (!jsonMaterials.contains(m)){
                    this.addMaterial(m);
                }
            }
        }
    }
}
