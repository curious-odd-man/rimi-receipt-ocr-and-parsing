# How to run project
1. Make sure that Tesseract https://github.com/tesseract-ocr/tesseract is installed.
2. If you want to run a test, either:
   1. run command `./gradlew test`
   2. in your IDE go to dedicated test and run it
3. If you want to run an app:
   1. Go to [application.yml](src%2Fmain%2Fresources%2Fapplication.yml)
   2. Change paths as you like - most importantly your pdf documents should be in `input-dir` property path
   3. run command `./gradlew bootRun` or start Main in your IDE

4. There are also additional mains for different purposes in [playground](src%2Fmain%2Fjava%2Fcom%2Fgithub%2Fcuriousoddman%2Freceipt%2Fplayground)
