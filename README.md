# jszuru

A pretty straight-forward java API for szurubooru-based image boards, mostly based on the python API by [sgsunder](https://github.com/sgsunder)

As of commit [542a37c](https://github.com/G1org1owo/jszuru/commit/542a37c32ef5b83c08de009f361489ffa9458147) the codebase isn't very java-like, but I hope I'll be able to work on it and improve it in order to have a complete and stable API.

## Dependencies
If you download the `embedded` version of the library you can go ahead and include your jar in your project without worrying about a thing, while if you download the `base` version, you'll have to include the following dependencies in your project

```xml
<!-- https://mvnrepository.com/artifact/com.google.code.gson/gson -->
<dependency>
    <groupId>com.google.code.gson</groupId>
    <artifactId>gson</artifactId>
    <version>2.10.1</version>
</dependency>

<!-- https://mvnrepository.com/artifact/org.apache.httpcomponents/httpclient -->
<dependency>
    <groupId>org.apache.httpcomponents</groupId>
    <artifactId>httpclient</artifactId>
    <version>4.5.14</version>
</dependency>

<!-- https://mvnrepository.com/artifact/org.apache.httpcomponents/httpmime -->
<dependency>
    <groupId>org.apache.httpcomponents</groupId>
    <artifactId>httpmime</artifactId>
    <version>4.5.14</version>
</dependency>
```

## Usage
In order to do anything, you must create an API instance:
```java
SzurubooruAPI mybooru = new SzurubooruAPIBuilder()
                              .setBaseUrl("https://mybooru.com:8080/")
                              .setUsername("G1org1o")
                              // You can use either a password or a token, if both are present the token will be used
                              .setToken("XXXXXXXX-XXXX-XXXX-XXXX-XXXXXXXXXXXX")
                              .setPassword("password")
                              // If you don't set the API uri it will default to '/api'
                              .setApiUri("/api")
                              .build();
```
or

```java
SzurubooruAPI mybooru = new SzurubooruAPI("https://mybooru.com:8080/",
                                          "G1org1o",
                                          "XXXXXXXX-XXXX-XXXX-XXXX-XXXXXXXXXXXX",
                                          "password",
                                          "/api");
```

To retrieve a `SzurubooruResource` (`SzurubooruTag` or `SzurubooruPost` for now) you simply need to invoke `SzurubooruAPI.get<Resource>()`:
```java
SzurubooruPost post = mybooru.getPost(727);
SzurubooruTag tag = mybooru.getTag("wysi");
```
Creating a new tag is just as easy, while creating a new post needs a few more steps:
```java
SzurubooruTag tag = mybooru.createTag("absurdres");

// Before creating a new post you must upload the file and get the token.
// You can do so by using either a java.io.File or a java.lang.String.
FileToken fileToken = mybooru.uploadFile("mikumybeloved.jpeg");
SzurubooruPost post = mybooru.createPost(fileToken, "safe");
```

If at any given point you felt the urge to retrieve any modification another user might have applied to your beloved tags, or if you decided to remove a tag from a post, you could do it through the various accessors and the methods `SzurubooruResource.push()` and `SzurubooruResource.pull()`
```java
// Retrieves the current state of the tag on the linked server,
// also useful for undoing any yet-to-be-pushed edits you might have done to a tag.
tag.pull()

// Retrieves the current tags for a post, then appends a new tag to the list.
List<SzurubooruTag> tags = post.getTags();
tags.add(mybooru.getTag("hatsune_miku_(vocaloid)"));

// Updates the tag list and then pushes the changes to the remote server.
post
  .setTags(tags)
  .push();
```

Lastly, if you want to perform a search for a specific tag or post, or just want to find similar images through an image search, you must use the methods `SzurubooruAPI.search<Resource>()` and `SzurubooruAPI.searchByImage()`
```java
SzurubooruTag[] tags = mybooru.searchTag("vocaloid");

List<SzurubooruSearchResult> similarImages = mybooru.searchByImage(fileToken);
```

## Useful links
- [pyszuru](https://github.com/sgsunder/python-szurubooru)
- [szurubooru API reference](https://github.com/rr-/szurubooru/blob/master/doc/API.md)
