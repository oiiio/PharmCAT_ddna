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

import org.pharmgkb.pharmcat.reporter.model.DrugLink;
import org.pharmgkb.pharmcat.reporter.model.result.GeneReport;
import org.pharmgkb.pharmcat.reporter.ReportContext;

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

  private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss");

  public HL7Format(Path outputPath) { super(outputPath); }

  public void write(ReportContext reportContext) throws IOException {
    try (BufferedWriter writer = Files.newBufferedWriter(getOutputPath(), StandardCharsets.UTF_8)) {
      writer.write(generateHL7(reportContext));
    }
  }

  private String generateHL7(ReportContext reportContext) {

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
    } catch (EncodingNotSupportedException e) {
        e.printStackTrace();
        return "";
    } catch (HL7Exception e) {
      e.printStackTrace();
      return "";
    }

    String sep = getFieldSeparator(t);
    String subSep = getSubFieldSeparator(t);

    String msh = generateMSHSegment(t, sep, subSep);
    String pid = generatePIDSegment(t, sep, subSep);
    String obr = generateOBRSegment(t, sep, subSep);
    String obx = generateOBXSegments(reportContext, t, sep, subSep);

    System.out.print(msh);
    System.out.print(pid);
    System.out.print(obr);

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

    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
    String msh = "MSH" + sep; //MSH-1 (MSH) and MSH-2 (field separator)
    
    try {
      msh += t.get("MSH-6") + sep; //MSH-3: sending application
      msh += t.get("MSH-4") + sep; //MSH-4
      msh += sep.repeat(2); //MSH-5 and MSH-6 (empty)
      msh += dateFormat.format(new Date()) + sep; //MSH-7: date
      msh += sep; //MSH-8: security (empty)
      msh += "ORU" + subsep + "R01" + sep; //MSH-9-1: msg type, and MSH-9-2: trigger event
      msh += System.currentTimeMillis() + sep; //MSH-10: control ID (use server time in millis)
      msh += "P" + sep; //MSH-11: processing ID (P = production)
      msh += "2.3" + sep.repeat(2) + "\n"; //MSH-12: HL7 version, MSH-13 and MSH-14 left emptty
    }
    catch(HL7Exception e) {
      e.printStackTrace();
    }
    return msh;
  }

  private String generatePIDSegment(Terser t, String sep, String subsep) {

    String pid = "PID" + sep;

    try {
      pid += t.get("PID-1") + sep; //PID-1
      pid += sep; //PID-2 blank
      pid += t.get("PID-2-1") + subsep.repeat(4) + t.get("PID-2-4") + sep; //PID-3-1 and PID-3-5
      pid += sep; //PID-4
      pid += t.get("PID-5-1") + subsep + t.get("PID-5-2") + sep; //PID-5-1 and PID-5-2
      pid += sep;
      pid += t.get("PID-7") + sep; //date of birth
      pid += t.get("PID-8") + "\n"; //gender
    }
    catch (HL7Exception e){
      e.printStackTrace();
    }

    return pid;
  }

  private String generateOBRSegment(Terser t, String sep, String subsep) {

    String obr = "OBR" + sep;
    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss");

    try {
      obr += t.get("OBR-1") + sep;
      obr += t.get("OBR-2") + sep;
      obr += System.currentTimeMillis() + sep;
      obr += t.get("OBR-4-1") + subsep + t.get("OBR-4-2") + sep;
      obr += sep.repeat(2); //5 and 6 unsupported
      obr += dateFormat.format(new Date()) + sep; //observation date and time
      obr += sep.repeat(6); //8 through 14 not needed
      obr += sep.repeat(1); //15
      obr += t.get("OBR-16-1") + subsep + t.get("OBR-16-2") + subsep + t.get("OBR-16-3") +
          subsep + t.get("OBR-16-4") + subsep.repeat(9) + t.get("OBR-16-9") + sep;
      obr += sep.repeat(5); //17 - 21 blank
      obr += dateFormat.format(new Date()) + sep; //22 - results time
      obr += sep.repeat(2); //23 and 24 blank
      obr += "F" + sep; //25: status set to Final
      obr += sep.repeat(2); //26 and 27 blank
      obr += t.get("OBR-28-1") + subsep + t.get("OBR-28-2") + subsep + t.get("OBR-28-3") + subsep +
          subsep.repeat(6) + t.get("OBR-28-9") + subsep.repeat(4) + t.get("OBR-28-13") + "\n";
    }
    catch (HL7Exception e) {
      e.printStackTrace();
    }

    return obr;
  }

  private String generateOBXSegments(ReportContext reportContext, Terser t, String sep, String subsep) {

    String obx = "";
    Collection<GeneReport> geneReports = reportContext.getGeneReports();
    int currentOBXSegment = 1;

    for (Iterator<GeneReport> i = geneReports.iterator(); i.hasNext();) {
      GeneReport report = i.next();
      obx += "OBX" + sep + (currentOBXSegment++) + "|ST|47998-0^Variant Display Name^LN|<observation sub-id>|" + report.getGene() + "\r\n";
      obx += "OBX" + sep + (currentOBXSegment++) +  "|CWE|48018-6^Gene studied^LN|<observation sub-id>|" + report.getGene() + "\r\n";

      SortedSet<DrugLink> relatedDrugs = report.getRelatedDrugs();
      for (Iterator<DrugLink> it = relatedDrugs.iterator(); it.hasNext();) {
        obx += "OBX|" + currentOBXSegment + "|CWE|51963-7^Medication Assessed^LN|<observation sub-id>|" + it.next().getName() + "\r\n";
            ++currentOBXSegment;
      }
    }
    return obx;
  }

  private String generateOBXDrugSegment(GeneReport report) {
    String drug = "";

    return drug;
  }

}