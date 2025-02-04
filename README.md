

# sham.software Mock SSH API

Mock SSH and SFTP testing library for running an SSH server in process. SFTP uses a local temporary directory.

## Development Notes
04.02.2025 1.0.0-RC2 is ready
* restored sftp
* introduced git-pack

02.02.2025 1.0.0-RC1 is ready
* swapping maverick-synergy-client for JSch (for testing)
* completing the mocking framework to survive basic checks from ssh-client
(greeting, prompt and echo-ing)
* support command or shell mode exclusively

25.01.2025 0.4.0 is ready
* revamped with latest versions of sshd-core and sshd-sftp.

Hope to have soon artifacts on MC.

**Table of Contents**

* [Example Usage] (#example-usage)
* [License] (#License)

## Getting it on your classpath

### Maven

```shell
git checkout https://github.com/janesser/sham-ssh
cd sham-ssh
mvn install
```

```xml
<dependency>
  <groupId>software.sham</groupId>
  <artifactId>sham-ssh</artifactId>
  <version>1.0.0-SNAPSHOT</version>
</dependency>
```

The artifacts are available in the [Maven Central Repository](http://search.maven.org/#search%7Cga%7C1%7Ca%3A%22sham-ssh%22).

### Manual

If you need to add this to your project manually, you can download it directly from the maven central repository:

[Download Jar](http://search.maven.org/remotecontent?filepath=software/sham/sham-ssh/0.1.0/sham-ssh-0.1.0.jar)

# License

   Copyright 2015 Ryan Hoegg.

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
