# BlueSoft Endurance #

BlueSoft Endurance is a set of libraries and frameworks designed to facilitate the creation of highly available, performant, and scalable web applications and web services.  The first few installments of the framework are instrumentation and memoryCache.  This is the instrumentation component and contains a few classes for dealing with functional concepts, timing, and concurrency.

## WARNING ##

This software is in a very early stage of development.  We will be improving it as we go and there may be significant changes between versions.  As long as we are in this stage of development the code should be considered extremely unstable and not ready for production use.

## Getting Started ##

### Maven ###
To utilize this library in a Maven project simply add the following to your pom:
```
#!xml
<repositories>
  <repository>
    <releases>
      <enabled>false</enabled>
    </releases>
    <snapshots>
      <enabled>true</enabled>
    </snapshots>
    <id>oss-snapshots</id>
    <name>Sonatype Open Source Software Repository - SNAPSHOTs</name>
    <url>https://oss.sonatype.org/content/repositories/snapshots/</url>
    <layout>default</layout>
  </repository>
</repositories>
<dependencies>
  <dependency>
    <groupId>com.bluesoftdev.endurance</groupId>
    <artifactId>instrumentation</artifactId>
    <version>1.0.0-SNAPSHOT</version>
  </dependency>
</dependencies>
```
### Gradle ###
To use it in a Gradle project:
```
#!groovy
repositories {
  maven {
    url 'https://oss.sonatype.org/content/repositories/snapshots/'
  }
}
dependencies {
  compile 'com.bluesoftdev.endurance:instrumentation:1.0.0-SNAPSHOT'
}
```
### Others ###
You can download the jar and sources from: [Repo](https://oss.sonatype.org/content/repositories/snapshots/com/bluesoftdev/endurance/instrumentation/1.0.0-SNAPSHOT/)
## Contribution guidelines ##

If you would like to contribute please feel free to submit pull requests.  Some guidelines:

* Tests: There must be tests to accompany any change.  Please check that there is sufficient code coverage by looking at the JaCoCo report.
* Code Format: please follow the same basic style as the rest of the code.  There is no strict format requirements but there may be in the future.
* Attribution and License: The license is Apache 2.0, all contributions will be licensed in the same way and you give BlueSoft Development, LLC ownership of the code.

## Support ##

To get support, submit a ticket on this bitbucket site or send me an email at danap@bluesoftdev.com