package com.fs.starfarer.api.impl.campaign.rulecmd;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.AdminData;
import com.fs.starfarer.api.characters.FullName;
import com.fs.starfarer.api.characters.OfficerDataAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.MutableStat;
import com.fs.starfarer.api.impl.campaign.ids.Strings;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.Misc.Token;
import com.fs.starfarer.campaign.CommDirectory;
import com.fs.starfarer.launcher.ModManager;
import com.fs.starfarer.rpg.Person;
import data.scripts.utilities.StringHelper;
import org.apache.log4j.Logger;

import java.awt.*;
import java.util.List;
import java.util.Map;

public class vic_PersonaChange extends BaseCommandPlugin {

    public static Logger log = Global.getLogger(vic_PersonaChange.class);
    protected static vic_personaChangeData temp = new vic_personaChangeData();
    public String
            male = "vic_PersonaChangeMale",
            female = "vic_PersonaChangeFemale",
            result = "vic_PersonaChangeResult",
            NEX_GO_BACK = "vic_PersonaChangeEnd",
            GO_BACK = "vic_PersonaChangeEnd",
            NEX_GO_BACK_NEW = "vic_PersonaChangeEndNew",
            GO_BACK_NEW = "vic_PersonaChangeEndNew",
            changeSelf = "vic_PersonaChangeYou",
            changeOfficer = "vic_PersonaChangeOfficer",
            changeAdmin = "vic_PersonaChangeAdmin";
    protected CampaignFleetAPI playerFleet;
    protected SectorEntityToken entity;
    protected FactionAPI playerFaction;
    protected FactionAPI entityFaction;
    protected TextPanelAPI text;
    protected OptionPanelAPI options;
    protected CargoAPI playerCargo;
    protected MemoryAPI memory;
    protected MarketAPI market;
    protected InteractionDialogAPI dialog;
    protected Map<String, MemoryAPI> memoryMap;
    protected FactionAPI faction;

    public vic_PersonaChange() {
    }

    public vic_PersonaChange(SectorEntityToken entity) {
        init(entity);
    }

    protected static void resetTmp() {
        if (temp != null) {
            temp.isPlayer = true;
            temp.personaToChange = null;
        }

    }

    public static TooltipMakerAPI.StatModValueGetter statPrinter(final boolean withNegative) {
        return new TooltipMakerAPI.StatModValueGetter() {
            public String getPercentValue(MutableStat.StatMod mod) {
                String prefix = mod.getValue() > 0 ? "+" : "";
                return prefix + (int) (mod.getValue()) + "%";
            }

            public String getMultValue(MutableStat.StatMod mod) {
                return Strings.X + "" + Misc.getRoundedValue(mod.getValue());
            }

            public String getFlatValue(MutableStat.StatMod mod) {
                String prefix = mod.getValue() > 0 ? "+" : "";
                return prefix + (int) (mod.getValue()) + "";
            }

            public Color getModColor(MutableStat.StatMod mod) {
                if (withNegative && mod.getValue() < 1f) return Misc.getNegativeHighlightColor();
                return null;
            }
        };
    }

    protected void init(SectorEntityToken entity) {

        memory = entity.getMemoryWithoutUpdate();
        this.entity = entity;
        playerFleet = Global.getSector().getPlayerFleet();
        playerCargo = playerFleet.getCargo();

        playerFaction = Global.getSector().getPlayerFaction();
        entityFaction = entity.getFaction();

        faction = entity.getFaction();

        market = entity.getMarket();
    }

    public boolean execute(String ruleId, InteractionDialogAPI dialog, List<Token> params, Map<String, MemoryAPI> memoryMap) {
        //super.execute(ruleId, dialog, params, memoryMap);

        this.dialog = dialog;
        this.memoryMap = memoryMap;

        String command = params.get(0).getString(memoryMap);
        if (command == null) return false;

        entity = dialog.getInteractionTarget();
        init(entity);

        memory = getEntityMemory(memoryMap);

        text = dialog.getTextPanel();
        options = dialog.getOptionPanel();

        switch (command) {
            case "vic_PerconaChangeChose":
                PerconaChangeChose();
                break;
            case "vic_PersonaChangeYou":
                PersonaChangeMenu();
                break;
            case "vic_PersonaChangeOfficer":
                PerconaChangeChosePersona(true);
                break;
            case "vic_PersonaChangeAdmin":
                PerconaChangeChosePersona(false);
                break;
            case "vic_PerconaChangeNotPLayer":
                PerconaChangeNotPLayer();
                break;
            case "vic_PersonaChangeMenu":
                PersonaChangeMenu();
                break;
            case "vic_PersonaChangeFemale":
                CommsSummon(FullName.Gender.FEMALE);
                break;
            case "vic_PersonaChangeMale":
                CommsSummon(FullName.Gender.MALE);
                break;
            case "vic_PersonaChangeConfirm":
                PersonaChangeConfirm();
                break;
            case "vic_PersonaChangeResult":
                PersonaChangeResult();
                break;
        }

        return true;
    }

    protected void PerconaChangeChose() {
        resetTmp();


        for (OfficerDataAPI officer : Global.getSector().getPlayerFleet().getFleetData().getOfficersCopy()) {
            officer.getPerson().removeTag("vic_personToChange");
        }

        for (AdminData admin : Global.getSector().getCharacterData().getAdmins()) {
            admin.getPerson().removeTag("vic_personToChange");
        }


        text.addPara("Who to change");

        options.clearOptions();
        options.addOption("Change yourself", changeSelf);
        options.addOption("Change officer", changeOfficer);
        options.addOption("Change administrator", changeAdmin);

        if (ModManager.getInstance().isModEnabled("nexerelin")) {
            options.addOption("Leave the Centre", NEX_GO_BACK);
        } else {
            options.addOption("Leave the Centre", GO_BACK);
        }

        if (Global.getSector().getPlayerFleet().getFleetData().getOfficersCopy().isEmpty()) {
            options.setEnabled(changeOfficer, false);
            options.setTooltip(changeOfficer, "You don't have any officers");
        }
        if (Global.getSector().getCharacterData().getAdmins().isEmpty()) {
            options.setEnabled(changeAdmin, false);
            options.setTooltip(changeAdmin, "You don't have any administrators");
        }


    }

    protected void PerconaChangeChosePersona(boolean isItOfficer) {
        CommDirectory directory = new CommDirectory();
        if (isItOfficer) {
            for (OfficerDataAPI officer : Global.getSector().getPlayerFleet().getFleetData().getOfficersCopy()) {
                officer.getPerson().addTag("vic_personToChange");
                directory.addPerson(officer.getPerson());
            }
        } else {
            for (AdminData admin : Global.getSector().getCharacterData().getAdmins()) {
                admin.getPerson().addTag("vic_personToChange");
                directory.addPerson(admin.getPerson());
            }
        }
        dialog.showCommDirectoryDialog(directory);
    }

    protected void PerconaChangeNotPLayer() {
        temp.personaToChange = entity.getActivePerson();
        temp.isPlayer = false;
        PersonaChangeMenu();
    }

    //generate menu
    protected void PersonaChangeMenu() {

        text.addPara(StringHelper.getString("vic_PersonaChange", "Choose gender"));

        options.clearOptions();

        options.addOption("Open \"Male\" section", male);
        options.addOption("Open \"Female\" section", female);

        if (playerCargo.getCredits().get() < 10000) {
            options.setEnabled(male, false);
            options.setTooltip(male, "Not enough credits.");
            options.setEnabled(female, false);
            options.setTooltip(female, "Not enough credits.");
        }

        if (ModManager.getInstance().isModEnabled("nexerelin")) {
            options.addOption("Leave the Centre", NEX_GO_BACK);
        } else {
            options.addOption("Leave the Centre", GO_BACK);
        }
    }

    //comms
    protected void CommsSummon(FullName.Gender gender) {

        CommDirectory directory = new CommDirectory();
        int number = 0;
        for (String s : Global.getSector().getPlayerFaction().getPortraits(gender).getItems()) {
            Person dude = new Person();
            dude.setPortraitSprite(s);
            dude.setRankId(null);
            dude.addTag("vic_PortraitHolder");
            dude.setName(new FullName(number + "-X", gender.name(), gender));
            directory.addPerson(dude);
            number++;
        }

        dialog.showCommDirectoryDialog(directory);
    }

    //portrait menu
    protected void PersonaChangeConfirm() {

        options.clearOptions();

        options.addOption("Confirm your choice", result);

        options.addOption("Return to the \"Male\" section", male);
        options.addOption("Return to the \"Female\" section", female);

        options.addOption("Close the list and leave the Centre", GO_BACK);
    }

    //result screen
    protected void PersonaChangeResult() {

        text.addPara(StringHelper.getString("vic_PersonaChange", "Result"));

        playerCargo.getCredits().subtract(10000);
        AddRemoveCommodity.addCreditsLossText(10000, text);

        if (temp.isPlayer)
            Global.getSector().getCharacterData().setPortraitName(entity.getActivePerson().getPortraitSprite());
        else {
            temp.personaToChange.setPortraitSprite(entity.getActivePerson().getPortraitSprite());
            for (OfficerDataAPI officer : Global.getSector().getPlayerFleet().getFleetData().getOfficersCopy()) {
                officer.getPerson().removeTag("vic_personToChange");
            }
            for (AdminData admin : Global.getSector().getCharacterData().getAdmins()) {
                admin.getPerson().removeTag("vic_personToChange");
            }
        }
        //Global.getSector().getCharacterData().setName(entity.getActivePerson().getName().getFirst(), entity.getActivePerson().getGender());

        options.clearOptions();

        options.addOption("Return to the \"Male\" section of the list", male);
        options.addOption("Return to the \"Female\" section of the list", female);

        if (ModManager.getInstance().isModEnabled("nexerelin")) {
            options.addOption("Leave the Centre", NEX_GO_BACK_NEW);
        } else {
            options.addOption("Leave the Centre", GO_BACK_NEW);
        }
    }

    protected static class vic_personaChangeData {
        public boolean isPlayer;
        public PersonAPI personaToChange;
    }
}