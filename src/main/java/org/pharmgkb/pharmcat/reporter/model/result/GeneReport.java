package org.pharmgkb.pharmcat.reporter.model.result;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import org.apache.commons.lang3.StringUtils;
import org.pharmgkb.common.comparator.HaplotypeNameComparator;
import org.pharmgkb.pharmcat.haplotype.NamedAlleleMatcher;
import org.pharmgkb.pharmcat.haplotype.model.GeneCall;
import org.pharmgkb.pharmcat.reporter.DiplotypeFactory;
import org.pharmgkb.pharmcat.reporter.VariantReportFactory;
import org.pharmgkb.pharmcat.reporter.model.DrugLink;
import org.pharmgkb.pharmcat.reporter.model.MessageAnnotation;
import org.pharmgkb.pharmcat.reporter.model.OutsideCall;
import org.pharmgkb.pharmcat.reporter.model.VariantReport;
import org.pharmgkb.pharmcat.util.Slco1b1AlleleMatcher;
import org.pharmgkb.pharmcat.util.Ugt1a1AlleleMatcher;


/**
 * This class is used to help collect Gene-related data for later reporting
 */
public class GeneReport implements Comparable<GeneReport> {
  // never display these genes in the gene call list
  public static final List<String> IGNORED_GENES = ImmutableList.of("G6PD", "HLA-A", "HLA-B");
  private static final List<String> sf_reducibleGeneCalls = ImmutableList.of("UGT1A1");
  private static final Set<String> sf_overrideDiplotypes = ImmutableSet.of("SLCO1B1");
  private static final String UNCALLED = "not called";
  public static  final String NA = "N/A";

  @Expose
  @SerializedName("geneSymbol")
  private final String f_gene;
  @Expose
  @SerializedName("chr")
  private String m_chr;
  @Expose
  @SerializedName("phased")
  private boolean m_phased = false;
  @Expose
  @SerializedName("callSource")
  private final CallSource f_callSource;
  @Expose
  @SerializedName("uncalledHaplotypes")
  private final SortedSet<String> m_uncalledHaplotypes = new TreeSet<>(HaplotypeNameComparator.getComparator());
  @Expose
  @SerializedName("messages")
  private final List<MessageAnnotation> m_messages = new ArrayList<>();
  private final List<DrugLink> m_relatedDrugs = new ArrayList<>();
  private final List<Diplotype> m_matcherDiplotypes = new ArrayList<>();
  @Expose
  @SerializedName("diplotypes")
  private final List<Diplotype> m_reporterDiplotypes = new ArrayList<>();
  @Expose
  @SerializedName("variants")
  private final List<VariantReport> m_variantReports = new ArrayList<>();
  @Expose
  @SerializedName("variantsOfInterest")
  private final List<VariantReport> m_variantOfInterestReports = new ArrayList<>();
  @Expose
  @SerializedName("highlightedVariants")
  private final List<String> m_highlightedVariants = new ArrayList<>();

  /**
   * public constructor
   */
  public GeneReport(String geneSymbol) {
    f_gene = geneSymbol;
    f_callSource = CallSource.NONE;
  }

  public GeneReport(GeneCall call) {
    f_gene = call.getGene();
    f_callSource = CallSource.MATCHER;
    try {
      m_chr = call.getChromosome();
      m_uncalledHaplotypes.clear();
      m_uncalledHaplotypes.addAll(call.getUncallableHaplotypes());
      m_phased = call.isPhased();

      VariantReportFactory variantReportFactory = new VariantReportFactory(f_gene, m_chr);
      call.getVariants().stream()
          .map(variantReportFactory::make).forEach(m_variantReports::add);
      call.getMatchData().getMissingPositions().stream()
          .map(variantReportFactory::make).forEach(m_variantReports::add);
      call.getVariantsOfInterest().stream()
          .map(variantReportFactory::make).forEach(m_variantOfInterestReports::add);

      // set the flag in reports for the variants with mismatched alleles
      call.getMatchData().getMismatchedPositions().stream()
          .flatMap(a -> m_variantReports.stream().filter(v -> v.getPosition() == a.getPosition()))
          .forEach(r -> r.setMismatch(true));
    } catch (IOException ex) {
      throw new RuntimeException("Could not start a gene report", ex);
    }
  }

  public GeneReport(OutsideCall call) {
    f_gene = call.getGene();
    f_callSource = CallSource.OUTSIDE;
  }

  public void setDiplotypes(DiplotypeFactory diplotypeFactory, GeneCall geneCall) {
    m_matcherDiplotypes.addAll(diplotypeFactory.makeDiplotypes(geneCall));

    // for UGT1A1 we need to calculate diplotypes slightly differently
    if (Ugt1a1AlleleMatcher.shouldBeUsedOn(this)) {
      Set<String> diplotypes = Ugt1a1AlleleMatcher.makeLookupCalls(this);
      m_reporterDiplotypes.addAll(diplotypeFactory.makeDiplotypes(diplotypes));
    } 
    else if (Slco1b1AlleleMatcher.shouldBeUsedOn(this)) {
      Slco1b1AlleleMatcher
          .makeLookupCalls(this, diplotypeFactory)
          .ifPresent(m_reporterDiplotypes::add);
    }
    else {
      m_reporterDiplotypes.addAll(diplotypeFactory.makeDiplotypes(geneCall));
    }
  }

  public void setDiplotypes(DiplotypeFactory diplotypeFactory, OutsideCall outsideCall) {
    diplotypeFactory.makeDiplotypes(outsideCall).forEach(d -> {
      m_matcherDiplotypes.add(d);
      m_reporterDiplotypes.add(d);
    });
  }

  /**
   * The gene symbol for this gene
   */
  public String getGene(){
    return f_gene;
  }

  /**
   * The chromosome this gene appears on
   */
  public String getChr() {
    return m_chr;
  }

  /**
   * The list of messages that apply to this gene
   */
  public List<MessageAnnotation> getMessages(){
    return m_messages;
  }

  public void addMessages(Collection<MessageAnnotation> messages) {
    if (messages == null) {
      return;
    }

    // separate the general messages from specific genotype call messages
    messages.forEach(ma -> {
      if (ma.getExceptionType().equals(MessageAnnotation.TYPE_GENOTYPE)) {
        String rsid = ma.getMatches().getVariant();

        Optional<String> call = getVariantReports().stream()
            .filter(v -> v.getDbSnpId() != null && v.getDbSnpId().matches(rsid) && !v.isMissing())
            .map(VariantReport::getCall)
            .reduce((a,b) -> {throw new RuntimeException();});
        String genotype;
        if (!call.isPresent() || StringUtils.isBlank(call.get())) {
          genotype = "missing";
        }
        else {
          genotype = rsid + call.get().replaceAll("[|/]", "/"+rsid);
        }
        m_highlightedVariants.add(genotype);

      }
      else {
        m_messages.add(ma);
      }
    });

  }

  public List<VariantReport> getVariantReports() {
    return m_variantReports;
  }

  public List<VariantReport> getVariantOfInterestReports() {
    return m_variantOfInterestReports;
  }

  /**
   * Gets the Set of Haplotypes the the matcher could not evaluate
   */
  public Set<String> getUncalledHaplotypes() {
    return m_uncalledHaplotypes;
  }

  public String toString() {
    return f_gene + " Report";
  }

  /**
   * True if the {@link NamedAlleleMatcher} has returned at least one call for this gene, false otherwise
   */
  public boolean isCalled() {
    return m_matcherDiplotypes.size() > 0 && m_matcherDiplotypes.stream().noneMatch(Diplotype::isUnknown);
  }

  /**
   * True if there is a diplotype for this gene that the reporter can use, false otherwise. The reporter may be able to 
   * use diplotype calls not made by the matcher (e.g. UGT1A1 or SLCO1B1)
   */
  public boolean isReportable() {
    return m_reporterDiplotypes.size() > 0;
  }

  @Override
  public int compareTo(GeneReport o) {
    return Objects.compare(getGene(), o.getGene(), String.CASE_INSENSITIVE_ORDER);
  }

  /**
   * Gets a list of {@link DrugLink} objects that are in the same guidelines as this gene
   * @return a list of {@link DrugLink} objects
   */
  public List<DrugLink> getRelatedDrugs() {
    return m_relatedDrugs;
  }

  /**
   * Adds the drugs in the given <code>guideline</code> to this report as {@link DrugLink} objects
   * @param guideline a GuidelineReport with relatedDrugs
   */
  public void addRelatedDrugs(DrugReport guideline) {

    guideline.getRelatedDrugs().stream()
        .map(d -> new DrugLink(d, guideline.getId(), guideline.isRxChange(), guideline.isRxPossible()))
        .forEach(m_relatedDrugs::add);
  }

  /**
   * True if the genotyping data in the report comes from outside PharmCAT, false if match is made by PharmCAT
   */
  public boolean isOutsideCall() {
    return f_callSource == CallSource.OUTSIDE;
  }

  public CallSource getCallSource() {
    return f_callSource;
  }

  /**
   * Gets a Set of the diplotype keys (Strings) that should be used when looking up matching annotation groups.
   * This method ensures all per-gene adjustments and fixes have been applied before it's time to match.
   * @return a Set of diplotype Strings in the form "GENE:
   */
  public Set<String> getDiplotypeLookupKeys() {
    Set<String> results;
    if (!isCalled()) {
      if (sf_overrideDiplotypes.contains(getGene()) && !m_reporterDiplotypes.isEmpty()) {
        results = m_reporterDiplotypes.stream().map(Diplotype::printLookupKey).collect(Collectors.toSet());
      }
      else {
        // only HLA-B uses Other/Other, all else is Unknown/Unknown
        String defaultUncalled = getGene().equals("HLA-B") ? "Other/Other" : "Unknown/Unknown";
        results = ImmutableSet.of(f_gene + ":" + defaultUncalled);
      }
    }
    else {
      if (getGene().equals("UGT1A1") && (!isPhased() || m_reporterDiplotypes.size() > 1)) {
        results = Ugt1a1AlleleMatcher.makeLookupCalls(this).stream()
            .map(c -> "UGT1A1:"+c).collect(Collectors.toSet());
      }
      else {
        results = m_reporterDiplotypes.stream().map(Diplotype::printLookupKey).collect(Collectors.toSet());
      }
    }
    return results;
  }

  /**
   * Used in the final report template in the Genotype Summary table in the "Genotype" column
   * @return a Collection of diplotype Strings (e.g. *2/*3, *4 (heterozygote))
   */
  public Collection<String> printDisplayCalls() {
    if (!isCalled()) {
      if (sf_overrideDiplotypes.contains(getGene()) && !m_reporterDiplotypes.isEmpty()) {
        return m_reporterDiplotypes.stream().sorted().map(Diplotype::printDisplay).collect(Collectors.toList());
      }
      return ImmutableList.of(UNCALLED);
    }
    else if (f_gene.equals("UGT1A1") && !isPhased()) {
      return m_matcherDiplotypes.stream()
          .flatMap(Diplotype::streamAllelesByZygosity)
          .distinct()
          .collect(Collectors.toList());
    }
    else if (isCallReducible()) {
      return ImmutableSet.of(
          m_matcherDiplotypes.stream()
              .map(Diplotype::printBare)
              .reduce(Diplotype.phasedReducer)
              .orElse(UNCALLED));
    }

    return m_matcherDiplotypes.stream().sorted().map(Diplotype::printDisplay).collect(Collectors.toList());
  }

  /**
   * Used in the final report template in the Genotype Summary table in the "Allele Functionality" column
   * @return a Collection of function Strings (e.g. Two no function alleles)
   */
  public Collection<String> printDisplayFunctions() {
    if (!isCalled()) {
      return ImmutableList.of(NA);
    }
    return m_reporterDiplotypes.stream().sorted().map(Diplotype::printFunctionPhrase).collect(Collectors.toList());
  }

  /**
   * Used in the final report template in the Genotype Summary table in the "Phenotype" column
   * @return a Collection of phenotype Strings (e.g. Poor Metabolizer)
   */
  public Collection<String> printDisplayPhenotypes() {
    if (!isCalled()) {
      return ImmutableList.of(NA);
    }
    return m_reporterDiplotypes.stream().sorted().map(Diplotype::getPhenotype).collect(Collectors.toList());
  }

  /**
   * Wether this gene has been marked as phased by the matcher
   */
  public boolean isPhased() {
    return m_phased;
  }

  /**
   * Can multiple phased diplotype calls be reduced down into the "+" notation.
   *
   * For example, 2 gene calls of *1/*2 and *1/*3 will be reduced to 1 call of *1/*2+*3.
   */
  private boolean isCallReducible() {
    return sf_reducibleGeneCalls.contains(getGene()) && isPhased();
  }

  /**
   * Is this gene ignored, meaning it shouldn't be included in reports
   * @return true if this gene should be left out of output reports
   */
  public boolean isIgnored() {
    return IGNORED_GENES.contains(getGene());
  }

  public List<Diplotype> getMatcherDiplotypes() {
    return m_matcherDiplotypes;
  }

  public List<Diplotype> getReporterDiplotypes() {
    return m_reporterDiplotypes;
  }

  /**
   * Get variants that should be shown separately in the report
   */
  public List<String> getHighlightedVariants() {
    return m_highlightedVariants;
  }

  /**
   * Is there any missing variant in this gene?
   * @return true if there is any missing variant
   */
  public String isMissingVariants() {
    if (getGene().equals("CYP2D6")) return NA;
    
    return m_variantReports.stream().anyMatch(VariantReport::isMissing) ? "Yes" : "No";
  }
}
