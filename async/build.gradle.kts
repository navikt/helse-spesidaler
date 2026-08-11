plugins {
    id("no.nav.helse.sas.sas-deployable")
}

sasDeployable {
    mainClass = "no.nav.helse.spesidaler.async.AppKt"
    imageName = "helse-spesidaler-async"
}

dependencies {
    implementation(libs.rapids.and.rivers)
    implementation(libs.tbd.libs.azure)

    testImplementation(libs.tbd.libs.rapids.and.rivers.test)
    testImplementation(libs.tbd.libs.mock.http.client)
    testImplementation(libs.mockk)
}
