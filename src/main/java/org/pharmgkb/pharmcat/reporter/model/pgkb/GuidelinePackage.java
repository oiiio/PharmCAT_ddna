package org.pharmgkb.pharmcat.reporter.model.pgkb;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import org.pharmgkb.pharmcat.reporter.model.PrescribingGuidanceSource;
import org.pharmgkb.pharmcat.reporter.model.cpic.Publication;


/**
 * This class packs together the main Guideline Annotation and the Groups of diplotype-specific annotations
 *
 * @author Ryan Whaley
 */
public class GuidelinePackage implements Comparable<GuidelinePackage> {
  @Expose
  @SerializedName("guideline")
  private DosingGuideline guideline;
  @Expose
  @SerializedName("recommendations")
  private List<RecommendationAnnotation> recommendations = new ArrayList<>();
  @Expose
  @SerializedName("citations")
  private List<Publication> citations = new ArrayList<>();


  /**
   * Private constructor for GSON;
   */
  private GuidelinePackage() {
  }


  public DosingGuideline getGuideline() {
    return guideline;
  }


  public List<RecommendationAnnotation> getRecommendations() {
    return recommendations;
  }

  public boolean hasRecommendations() {
    return guideline.isRecommendation() && recommendations != null && !recommendations.isEmpty();
  }


  public List<Publication> getCitations() {
    return citations;
  }

  
  public Set<String> getGenes() {
    return guideline.getRelatedGenes().stream()
        .map(AccessionObject::getSymbol)
        .collect(Collectors.toSet());
  }

  public Set<String> getDrugs() {
    return guideline.getRelatedChemicals().stream()
        .map(AccessionObject::getName)
        .collect(Collectors.toSet());
  }


  public boolean isDataSourceType(PrescribingGuidanceSource type) {
    return guideline != null && type.matches(guideline);
  }

  @Override
  public String toString() {
    if (guideline != null) {
      String chemicals = recommendations.stream()
          .flatMap((r) -> r.getRelatedChemicals().stream())
          .map(AccessionObject::getName)
          .distinct()
          .sorted()
          .collect(Collectors.joining("_"));
      return String.format(
          "%s_%s_%s_and_%s.json",
          guideline.getSource(),
          guideline.getObjCls(),
          chemicals,
          guideline.getRelatedGenes().stream().map(AccessionObject::getSymbol).collect(Collectors.joining("_"))
      ).replaceAll("[\\s\\/]", "_");
    }
    else {
      return super.toString();
    }
  }

  @Override
  public int compareTo(GuidelinePackage o) {
    return toString().compareToIgnoreCase(o.toString());
  }
}
