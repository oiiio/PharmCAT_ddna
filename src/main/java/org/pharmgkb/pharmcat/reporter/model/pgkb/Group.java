
package org.pharmgkb.pharmcat.reporter.model.pgkb;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import javax.annotation.Nonnull;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import org.pharmgkb.pharmcat.reporter.model.result.Diplotype;
import org.pharmgkb.pharmcat.reporter.model.result.Genotype;


/**
 * PharmGKB Guideline Annotation Group Model
 */
public class Group implements Comparable<Group> {

  @Expose
  @SerializedName("id")
  private String id;
  @Expose
  @SerializedName("name")
  private String name;
  @Expose
  @SerializedName("strength")
  private OntologyTerm strength;
  @Expose
  @SerializedName("rxChangeStatus")
  private OntologyTerm rxChange;
  @Expose
  @SerializedName("genePhenotypes")
  private List<String> genePhenotypes = new ArrayList<>();
  @SerializedName("metabolizerStatus")
  @Expose
  private Markdown metabolizerStatus;
  @SerializedName("recommendation")
  @Expose
  private Markdown recommendation;
  @SerializedName("implications")
  @Expose
  private Markdown implications;
  @SerializedName("activityScore")
  @Expose
  private Markdown activityScore;

  private Set<String> matchingFunctionKeys = new TreeSet<>();
  private Set<Diplotype> matchingDiplotypes = new TreeSet<>();
  private final Set<Genotype> matchingGenotypes = new HashSet<>();


  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }


  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }


  public List<String> getGenePhenotypes() {
    return genePhenotypes;
  }

  public void setGenePhenotypes(List<String> genePhenotypes) {
    this.genePhenotypes = genePhenotypes;
  }


  public OntologyTerm getStrength() {
    return strength;
  }

  public void setStrength(OntologyTerm strength) {
    this.strength = strength;
  }


  public OntologyTerm getRxChange() {
    return rxChange;
  }

  public void setRxChange(OntologyTerm rxChange) {
    this.rxChange = rxChange;
  }


  @Override
  public int compareTo(@Nonnull Group o) {

    if (id == null) {
      return -1;
    }
    else if (o.id == null) {
      return 1;
    }
    else {
      return id.compareTo(o.getId());
    }
  }

  @Override
  public String toString() {
    return getName();
  }

  /**
   * Matches a given function key. The function key lists the gene symbol and functions for the allels on that gene,
   * separated by a <code>/</code> if more than one. The functions are also sorted alphabetically.
   * @param functionKey a String in the format <code>GENE:function 1/function 2</code>
   * @return true if the function key appears in this annotation group
   */
  public boolean matchesKey(String functionKey) {
    return getGenePhenotypes().contains(functionKey);
  }

  public Markdown getMetabolizerStatus() {
    return metabolizerStatus;
  }

  public void setMetabolizerStatus(Markdown metabolizerStatus) {
    this.metabolizerStatus = metabolizerStatus;
  }

  public Set<String> getMatchingFunctionKeys() {
    return matchingFunctionKeys;
  }

  public void setMatchingFunctionKeys(Set<String> matchingFunctionKeys) {
    this.matchingFunctionKeys = matchingFunctionKeys;
  }

  public void addMatchingFunctionKey(String functionKey) {
    this.matchingFunctionKeys.add(functionKey);
  }

  public Set<Diplotype> getMatchingDiplotypes() {
    return matchingDiplotypes;
  }

  public void setMatchingDiplotypes(Set<Diplotype> matchingDiplotypes) {
    this.matchingDiplotypes = matchingDiplotypes;
  }

  public void addMatchingDiplotype(Diplotype diplotype) {
    this.matchingDiplotypes.add(diplotype);
  }

  public Markdown getRecommendation() {
    return recommendation;
  }

  public void setRecommendation(Markdown recommendation) {
    this.recommendation = recommendation;
  }

  public Markdown getImplications() {
    return implications;
  }

  public void setImplications(Markdown implications) {
    this.implications = implications;
  }

  public Markdown getActivityScore() {
    return activityScore;
  }

  public void setActivityScore(Markdown activityScore) {
    this.activityScore = activityScore;
  }

  public Set<Genotype> getMatchingGenotypes() {
    return matchingGenotypes;
  }

  public void addMatchingGenotype(Genotype genotype) {
    matchingGenotypes.add(genotype);
  }
}