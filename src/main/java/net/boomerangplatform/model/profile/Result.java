package net.boomerangplatform.model.profile;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Result {

  @JsonProperty("address_business_country")
  private String addressBusinessCountry;

  @JsonProperty("address_business_locality")
  private String addressBusinessLocality;

  @JsonProperty("address_business_state")
  private String addressBusinessState;

  @JsonProperty("address_business_zip")
  private String addressBusinessZip;

  @JsonProperty("address_business_location")
  private String addressBusinessLocation;

  @JsonProperty("hrOrganizationCode")
  private String hrOrganizationCode;

  @JsonProperty("callupName")
  private String callupName;

  @JsonProperty("nameDisplay")
  private String nameDisplay;

  @JsonProperty("co")
  private String co;

  @JsonProperty("dept_code")
  private String deptCode;

  @JsonProperty("employeeType_code")
  private String employeeTypeCode;

  @JsonProperty("languages_lang")
  private List<String> languagesLang;

  @JsonProperty("mail")
  List<String> mail;

  @JsonProperty("manager")
  private String manager;

  @JsonProperty("name_last")
  private String nameLast;

  @JsonProperty("notesEmail")
  private String notesEmail;

  @JsonProperty("notesEmailWithDomain")
  private String notesEmailWithDomain;

  @JsonProperty("org_title")
  private String orgTitle;

  @JsonProperty("org_title_bu")
  private String orgTitleBU;

  @JsonProperty("practiceAlignment")
  private String practiceAlignment;

  @JsonProperty("preferredIdentity")
  private String preferredIdentity;

  @JsonProperty("role")
  private String role;

  @JsonProperty("serial")
  private String serial;

  @JsonProperty("telephone_itn")
  private String telephoneITN;

  @JsonProperty("telephone_office")
  private String telephoneOffice;

  @JsonProperty("telephone_tieline")
  private String telephoneTieline;

  @JsonProperty("telephone_mobile")
  private String telephoneMobile;

  @JsonProperty("workLocation_office")
  private String workLocationOffice;

  @JsonProperty("uid")
  private String uid;

  @JsonProperty("id")
  private String id;

  @JsonProperty("score")
  private float score;

  @JsonProperty("resultLevel")
  private int resultLevel;

  public String getAddressBusinessCountry() {
    return addressBusinessCountry;
  }

  public void setAddressBusinessCountry(String addressBusinessCountry) {
    this.addressBusinessCountry = addressBusinessCountry;
  }

  public String getAddressBusinessLocality() {
    return addressBusinessLocality;
  }

  public void setAddressBusinessLocality(String addressBusinessLocality) {
    this.addressBusinessLocality = addressBusinessLocality;
  }

  public String getAddressBusinessState() {
    return addressBusinessState;
  }

  public void setAddressBusinessState(String addressBusinessState) {
    this.addressBusinessState = addressBusinessState;
  }

  public String getAddressBusinessZip() {
    return addressBusinessZip;
  }

  public void setAddressBusinessZip(String addressBusinessZip) {
    this.addressBusinessZip = addressBusinessZip;
  }

  public String getAddressBusinessLocation() {
    return addressBusinessLocation;
  }

  public void setAddressBusinessLocation(String addressBusinessLocation) {
    this.addressBusinessLocation = addressBusinessLocation;
  }

  public String getHrOrganizationCode() {
    return hrOrganizationCode;
  }

  public void setHrOrganizationCode(String hrOrganizationCode) {
    this.hrOrganizationCode = hrOrganizationCode;
  }

  public String getCallupName() {
    return callupName;
  }

  public void setCallupName(String callupName) {
    this.callupName = callupName;
  }

  public String getNameDisplay() {
    return nameDisplay;
  }

  public void setNameDisplay(String nameDisplay) {
    this.nameDisplay = nameDisplay;
  }

  public String getCo() {
    return co;
  }

  public void setCo(String co) {
    this.co = co;
  }

  public String getDeptCode() {
    return deptCode;
  }

  public void setDeptCode(String deptCode) {
    this.deptCode = deptCode;
  }

  public String getEmployeeTypeCode() {
    return employeeTypeCode;
  }

  public void setEmployeeTypeCode(String employeeTypeCode) {
    this.employeeTypeCode = employeeTypeCode;
  }

  public List<String> getMail() {
    return mail;
  }

  public void setMail(List<String> mail) {
    this.mail = mail;
  }

  public String getManager() {
    return manager;
  }

  public void setManager(String manager) {
    this.manager = manager;
  }

  public String getNameLast() {
    return nameLast;
  }

  public void setNameLast(String nameLast) {
    this.nameLast = nameLast;
  }

  public String getNotesEmail() {
    return notesEmail;
  }

  public void setNotesEmail(String notesEmail) {
    this.notesEmail = notesEmail;
  }

  public String getNotesEmailWithDomain() {
    return notesEmailWithDomain;
  }

  public void setNotesEmailWithDomain(String notesEmailWithDomain) {
    this.notesEmailWithDomain = notesEmailWithDomain;
  }

  public String getOrgTitle() {
    return orgTitle;
  }

  public void setOrgTitle(String orgTitle) {
    this.orgTitle = orgTitle;
  }

  public String getOrgTitleBU() {
    return orgTitleBU;
  }

  public void setOrgTitleBU(String orgTitleBU) {
    this.orgTitleBU = orgTitleBU;
  }

  public String getPracticeAlignment() {
    return practiceAlignment;
  }

  public void setPracticeAlignment(String practiceAlignment) {
    this.practiceAlignment = practiceAlignment;
  }

  public String getPreferredIdentity() {
    return preferredIdentity;
  }

  public void setPreferredIdentity(String preferredIdentity) {
    this.preferredIdentity = preferredIdentity;
  }

  public String getRole() {
    return role;
  }

  public void setRole(String role) {
    this.role = role;
  }

  public String getSerial() {
    return serial;
  }

  public void setSerial(String serial) {
    this.serial = serial;
  }

  public String getTelephoneMobile() {
    return telephoneMobile;
  }

  public void setTelephoneMobile(String telephoneMobile) {
    this.telephoneMobile = telephoneMobile;
  }

  public String getUid() {
    return uid;
  }

  public void setUid(String uid) {
    this.uid = uid;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public float getScore() {
    return score;
  }

  public void setScore(float score) {
    this.score = score;
  }

  public int getResultLevel() {
    return resultLevel;
  }

  public void setResultLevel(int resultLevel) {
    this.resultLevel = resultLevel;
  }

  public List<String> getLanguagesLang() {
    return languagesLang;
  }

  public void setLanguagesLang(List<String> languagesLang) {
    this.languagesLang = languagesLang;
  }

  public String getTelephoneITN() {
    return telephoneITN;
  }

  public void setTelephoneITN(String telephoneITN) {
    this.telephoneITN = telephoneITN;
  }

  public String getTelephoneOffice() {
    return telephoneOffice;
  }

  public void setTelephoneOffice(String telephoneOffice) {
    this.telephoneOffice = telephoneOffice;
  }

  public String getTelephoneTieline() {
    return telephoneTieline;
  }

  public void setTelephoneTieline(String telephoneTieline) {
    this.telephoneTieline = telephoneTieline;
  }

  public String getWorkLocationOffice() {
    return workLocationOffice;
  }

  public void setWorkLocationOffice(String workLocationOffice) {
    this.workLocationOffice = workLocationOffice;
  }



}
