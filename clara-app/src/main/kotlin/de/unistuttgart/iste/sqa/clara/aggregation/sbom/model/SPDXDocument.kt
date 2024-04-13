package de.unistuttgart.iste.sqa.clara.aggregation.sbom.model

import com.fasterxml.jackson.annotation.JsonProperty

data class SPDXDocument(
    @JsonProperty("spdxVersion")
    val spdxVersion: String,
    @JsonProperty("dataLicense")
    val dataLicense: String,
    @JsonProperty("SPDXID")
    val spdxId: String,
    @JsonProperty("name")
    val name: String,
    @JsonProperty("documentNamespace")
    val documentNamespace: String,
    @JsonProperty("creationInfo")
    val creationInfo: CreationInfo,
    @JsonProperty("packages")
    val packages: List<SPDXPackage>,
    @JsonProperty("files")
    val files: List<SPDXFile>,
    @JsonProperty("hasExtractedLicensingInfos")
    val hasExtractedLicensingInfos: List<ExtractingLicensingInfo>?,
    @JsonProperty("relationships")
    val relationships: List<Relationship>,
)

data class SPDXFile(
    @JsonProperty("fileName")
    val fileName: String,
    @JsonProperty("SPDXID")
    val spdxId: String,
    @JsonProperty("fileTypes")
    val fileTypes: List<String>?,
    @JsonProperty("checksums")
    val checksums: List<Checksum>?,
    @JsonProperty("licenseConcluded")
    val licenseConcluded: String?,
    @JsonProperty("copyrightText")
    val copyrightText: String?,
    @JsonProperty("comment")
    val comment: String?,
)

data class SPDXPackage(
    @JsonProperty("name")
    val name: String,
    @JsonProperty("SPDXID")
    val spdxId: String,
    @JsonProperty("versionInfo")
    val versionInfo: String,
    @JsonProperty("supplier")
    val supplier: String,
    @JsonProperty("originator")
    val originator: String?,
    @JsonProperty("downloadLocation")
    val downloadLocation: String,
    @JsonProperty("filesAnalyzed")
    val filesAnalyzed: Boolean,
    @JsonProperty("checksums")
    val checksums: List<Checksum>?,
    @JsonProperty("sourceInfo")
    val sourceInfo: String?,
    @JsonProperty("licenseConcluded")
    val licenseConcluded: String?,
    @JsonProperty("licenseDeclared")
    val licenseDeclared: String?,
    @JsonProperty("copyrightText")
    val copyrightText: String?,
    @JsonProperty("externalRefs")
    val externalRefs: List<ExternalRef>,
    @JsonProperty("packageVerificationCode")
    val packageVerificationCode: PackageVerificationCode?,
    @JsonProperty("primaryPackagePurpose")
    val primaryPackagePurpose: String?,
)

data class ExternalRef(
    @JsonProperty("referenceCategory")
    val referenceCategory: String,
    @JsonProperty("referenceType")
    val referenceType: String,
    @JsonProperty("referenceLocator")
    val referenceLocator: String,
)

data class Checksum(
    @JsonProperty("algorithm")
    val algorithm: String,
    @JsonProperty("checksumValue")
    val checksumValue: String,
)

data class ExtractingLicensingInfo(
    @JsonProperty("licenseId")
    val licenseId: String,
    @JsonProperty("extractedText")
    val extractedText: String,
)

data class Relationship(
    @JsonProperty("spdxElementId")
    val spdxElementId: String,
    @JsonProperty("relatedSpdxElement")
    val relatedSpdxElement: String,
    @JsonProperty("relationshipType")
    val relationshipType: String,
    @JsonProperty("comment")
    val comment: String?,
)

data class PackageVerificationCode(
    @JsonProperty("packageVerificationCodeValue")
    val packageVerificationCodeValue: String?,
)

data class CreationInfo(
    @JsonProperty("licenseListVersion")
    val licenseListVersion: String,
    @JsonProperty("creators")
    val creators: List<String>,
    @JsonProperty("created")
    val created: String,
)