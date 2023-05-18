package org.pharmgkb.pharmcat.reporter.format;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.io.BufferedWriter;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.SortedSet;
import java.util.StringJoiner;
import java.util.HashMap;
import java.util.Map;

import org.pharmgkb.pharmcat.reporter.model.DrugLink;
import org.pharmgkb.pharmcat.reporter.model.result.GeneReport;
import org.pharmgkb.pharmcat.reporter.ReportContext;
import org.pharmgkb.pharmcat.phenotype.Phenotyper;

import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.parser.EncodingNotSupportedException;
import ca.uhn.hl7v2.HapiContext;
import ca.uhn.hl7v2.parser.Parser;
import ca.uhn.hl7v2.util.Terser;
import ca.uhn.hl7v2.model.Message;
import ca.uhn.hl7v2.DefaultHapiContext;


/**
 * An HL7/FHIR formatted version of {@link ReportContext} data.
 */
public class HL7Format extends AbstractFormat {

  private static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss");

  private static Map<String, String> geneToInteractionType = new HashMap<>() {{
    put("CYP2B6", "Metabolizer");
    put("CYP2C9", "Metabolizer");
    put("CYP2C19", "Metabolizer");
    put("CYP2D6", "Metabolizer");
    put("CYP3A5", "Metabolizer");
    put("DPYD", "Metabolizer");
    put("NUDT15", "Metabolizer");
    put("TPMT", "Metabolizer");
    put("UGT1A1", "Metabolizer");
    put("ABCG2", "Transport");
    put("SLCO1B1", "Transport");
    put("CACNA1S", "Risk");
    put("G6PD", "Risk");
    put("HLA-A", "Risk");
    put("MT-RNR1", "Risk");
    put("RYR1", "Risk");
    put("CFTR", "Efficacy");
  }};

  private static Map<String, String> interactionTypeToEpicCode = new HashMap<>() {{
    put("Metabolizer", "53040-2");
    put("Transport", "51961-1");
    put("Risk", "83009-1");
    put("Efficacy", "51961-1");
  }};

  private static Map<String, String> interactionTypeToEpicHeader = new HashMap<>() {{
    put("Metabolizer", "Genetic Variation's Effect on Drug Metabolism");
    put("Transport", "Genetic Variation's Effect on Drug Transport");
    put("Risk", "Genetic Variation's Effect on High-Risk");
    put("Efficacy", "Genetic Variation's Effect on Drug Efficacy");
  }};
  
  public HL7Format(Path outputPath) { super(outputPath); }

  public void write(ReportContext reportContext) throws IOException {
    try (BufferedWriter writer = Files.newBufferedWriter(getOutputPath(), StandardCharsets.UTF_8)) {
      writer.write(generateHL7(reportContext));
    }
  }

  private String generateHL7(ReportContext reportContext) {

    //will ultimately be passed in
    String msg = "MSH|^~\\&|PENNCHART|UPHS||PharmCAT|20220315141017|BLEZNUCJ|ORM^O01|624|T|2.3\r" +
        "PID|1|8643070857^^^UID^UID|8643070857^^^UID^UID||ZZZTST^GENEONE^^^||20000915|M|\r" +
        "ORC|NW|620941^EPC||200091116|||^^^20220315^^RI^^||20220315141010|BLEZNUCJ^BLEZNUCK^JOSEPH^P^||1841226024^NATHANSON^KATHERINE^LEAH^^MD^^^NPI^^^^NPI|^^^ENT^^^^^MEDICAL GENETICS PERELMAN|(800)789-7366^^^^^800^7897366||||PC0T2HU0^PC0T2HU0^^1^INITIAL DEPARTMENT|||||||||||O|Protocol\r" +
        "OBR|1|620941^EPC||PCAT^PharmCAT Generic Order^PharmCAT^^PharmCAT Generic Order||20220315|||||||||^^^SALIVA&SALIVA|1841226024^NATHANSON^KATHERINE^LEAH^^MD^^^NPI^^^^NPI|(800)789-7366^^^^^800^7897366|||||||Lab|||^^^20220315^^RI^^|1144657396^ASHER^STEPHANIE^^^^^^NPI^^^^NPI||||||||20220315";

    HapiContext context = new DefaultHapiContext();

    Parser p = context.getGenericParser();
    Terser t;
    Message m;

    try {
      m = p.parse(msg);
      t = new Terser(m);
    }
    catch (EncodingNotSupportedException e) {
      e.printStackTrace();
      return "";
    }
    catch (HL7Exception e) {
      e.printStackTrace();
      return "";
    }

    String sep = getFieldSeparator(t);
    String subSep = getSubFieldSeparator(t);

    String msh = generateMSHSegment(t, sep, subSep);
    String pid = generatePIDSegment(t, sep, subSep);
    String obr = generateOBRSegment(t, sep, subSep);
    String obx = generateOBXSegments(reportContext, t, sep, subSep);

    return msh + pid + obr + obx;
  }

  private String getFieldSeparator(Terser t) {
    try {
      return t.get("MSH-1");
    }
    catch (HL7Exception e) {
      e.printStackTrace();
      return "|"; //default
    }
  }

  private String getSubFieldSeparator(Terser t) {
    try {
      return t.get("MSH-2").substring(0, 1);
    }
    catch (HL7Exception e) {
      e.printStackTrace();
      return "^"; //default
    }
  }

  private String generateMSHSegment(Terser t, String sep, String subsep) {
    StringJoiner msh_joiner = new StringJoiner(sep);

    try {
      msh_joiner.add("MSH");
      msh_joiner.add(t.get("MSH-2"));
      msh_joiner.add(t.get("MSH-6")); //MSH-3: sending application
      msh_joiner.add(t.get("MSH-4"));
      msh_joiner.add("").add("");
      msh_joiner.add(dateFormat.format(new Date())); //MSH-7: date
      msh_joiner.add("");
      msh_joiner.add("ORU" + subsep + "R01"); //MSH-9-1: msg type, and MSH-9-2: trigger event
      msh_joiner.add(Long.toString(System.currentTimeMillis())); //MSH-10: control ID (use server time in millis)
      msh_joiner.add("P"); //MSH-11: processing ID (P = production)
      msh_joiner.add("2.3").add("").add("\n"); //MSH-12: HL7 version, MSH-13 and MSH-14 left empty
    }
    catch(HL7Exception e) {
      e.printStackTrace();
    }
    return msh_joiner.toString();
  }

  private String generatePIDSegment(Terser t, String sep, String subsep) {
    StringJoiner pid_joiner = new StringJoiner(sep);

    try {
      pid_joiner.add("PID");
      pid_joiner.add(t.get("PID-1"));
      pid_joiner.add("");
      pid_joiner.add(t.get("PID-2-1") + subsep.repeat(4) + t.get("PID-2-4")); //PID-3-1 and PID-3-5
      pid_joiner.add("");
      pid_joiner.add(t.get("PID-5-1") + subsep + t.get("PID-5-2"));
      pid_joiner.add("");
      pid_joiner.add(t.get("PID-7")); //PID-7: DOB
      pid_joiner.add(t.get("PID-8") + "\n"); //PID-8: gender
    }
    catch (HL7Exception e){
      e.printStackTrace();
    }
    return pid_joiner.toString();
  }

  private String generateOBRSegment(Terser t, String sep, String subsep) {
    StringJoiner obr_joiner = new StringJoiner(sep);

    try {
      obr_joiner.add("OBR");
      obr_joiner.add(t.get("OBR-1"));
      obr_joiner.add(t.get("OBR-2"));
      obr_joiner.add(Long.toString(System.currentTimeMillis()));
      obr_joiner.add(t.get("OBR-4-1") + subsep + t.get("OBR-4-2"));
      obr_joiner.add("").add("");
      obr_joiner.add(dateFormat.format(new Date())); //Observation date & time
      obr_joiner.add("").add("").add("").add("").add("").add("").add("");
      obr_joiner.add(t.get("OBR-16-1") + subsep + t.get("OBR-16-2") + subsep + t.get("OBR-16-3") +
          subsep + t.get("OBR-16-4") + subsep.repeat(9) + t.get("OBR-16-9"));
      obr_joiner.add("").add("").add("").add("").add("");
      obr_joiner.add(dateFormat.format(new Date()));
      obr_joiner.add("").add("");
      obr_joiner.add("F"); //status to Final
      obr_joiner.add("").add("");
      obr_joiner.add(t.get("OBR-28-1") + subsep + t.get("OBR-28-2") + subsep + t.get("OBR-28-3") + subsep +
          subsep.repeat(6) + t.get("OBR-28-9") + subsep.repeat(4) + t.get("OBR-28-13") + "\n");
    }
    catch (HL7Exception e) {
      e.printStackTrace();
    }

    return obr_joiner.toString();
  }

  private String generateOBXSegments(ReportContext reportContext, Terser t, String sep, String subsep) {

    String obx = "";
    StringJoiner obx_joiner = new StringJoiner(sep);

    Collection<GeneReport> geneReports = reportContext.getGeneReports();
    int currentOBXSegment = 1;

    for (Iterator<GeneReport> i = geneReports.iterator(); i.hasNext();) {
      GeneReport report = i.next();

      obx += "OBX" + sep + (currentOBXSegment++) + sep
          + "ST" + sep + "47998-0^Variant Display Name^LN" + sep
          + "sub-id" + sep + report.getGene() +
          sep.repeat(6) + "F\r\n";

      obx_joiner.add("OBX").add(Integer.toString(currentOBXSegment++));
      obx_joiner.add("ST").add("47998-0^Variant Display Name^LN").add("sub-id").add(report.getGene());
      obx_joiner.add("").add("").add("").add("").add("").add("").add("F\r\n");

      obx += "OBX" + sep + (currentOBXSegment++) + sep
          +  "CWE" + sep + "48018-6^Gene studied^LN" + sep
          + "sub-id" + sep + report.getGene()
          + sep.repeat(6) + "F\r\n";

      obx_joiner.add("OBX").add(Integer.toString(currentOBXSegment++));
      obx_joiner.add("CWE").add("48018-6^Gene studied^LN").add("sub-id").add(report.getGene());
      obx_joiner.add("").add("").add("").add("").add("").add("").add("F\r\n");

      obx += "OBX" + sep + (currentOBXSegment++) + sep
          + "ST" + sep + "84413-4^Genotype display name^LN" + sep
          + "sub-id" + sep + report.getReporterDiplotypes().get(0).printDisplay()
          + sep.repeat(6) + "F\r\n";

      obx += "OBX" + sep + (currentOBXSegment++) + sep
          + "ST" + sep + "53040-2^Genetic Variation's Effect of Drug Metabolism^LN" + sep
          + "sub-id" + sep + report.getReporterDiplotypes().get(0).printPhenotypes() + sep
          + sep.repeat(6) + "F\r\n";

      SortedSet<DrugLink> relatedDrugs = report.getRelatedDrugs();
      for (Iterator<DrugLink> it = relatedDrugs.iterator(); it.hasNext();) {
        obx += "OBX" + sep + (currentOBXSegment++) + sep
            + "CWE" + sep + "51963-7^Medication Assessed^LN" + sep
            +"sub-id" + sep + it.next().getName() + sep.repeat(6) + "F\r\n";
      }
    }
    return obx;
  }
}