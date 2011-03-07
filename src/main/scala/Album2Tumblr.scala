import collection.JavaConversions._
import io.Source
import xml.{NodeSeq, XML, Elem, Node}
import tools.nsc.io.{Path, File, Directory}
import java.io._
import java.nio.charset.Charset
import java.text.SimpleDateFormat
import java.util.{ArrayList, Calendar}

import org.apache.http.impl.client.DefaultHttpClient
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.BufferedHttpEntity
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;

import com.drew.metadata._
import com.drew.metadata.exif._
import com.drew.metadata.iptc._
import com.drew.imaging._

object Album2Tumblr {

  def main(args: Array[String]) {
    require(args.length == 3)
    val name = args(0)     // tumblr name
    val email = args(1)    // tumblr email
    val password = args(2) // tumblr password

    val tumblrUrl = "http://www.tumblr.com/api/write"

    import2Tumblr(Path("/home/shun/public_html/img"),
      write(tumblrUrl, name, email, password, _))
  }

  // get pictures recursively and post them to tumblr
  def import2Tumblr(path: Path, post: File => Unit): Unit = path.isFile match {
    case true => post(path.toFile)
    case false => path.walkFilter(
      p => p.isDirectory || p.extension.equalsIgnoreCase("jpg"))
      .foreach(import2Tumblr(_, post))
  }

  // write to tumblr
  def write(url: String, name: String, email: String, password: String, file: File) {
    val caption = getCaption(file)
    val date = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss").format(getDate(file))
    println(file)
    println(caption)
    println(date)

    val httpClient = new DefaultHttpClient
    val httpPost = new HttpPost(url)

    // http://www.tumblr.com/docs/en/api#api_write
    val params = Map(
      "email" -> email,
      "password" -> password,
      "type" -> "photo",
      "generator" -> "Shun Album",
      "date" -> date,
      "caption" -> caption,
      "group" -> "%s.tumblr.com".format(name),
      "send-to-twitter" -> "no"
    )

    // prepare multipart/form-data
    val entity = new MultipartEntity
    params.map(pair => entity.addPart(pair._1,
      new StringBody(pair._2, Charset.forName("UTF-8"))))

    val data = new FileBody(new java.io.File(file.path))
    entity.addPart("data", data)

    httpPost.setEntity(entity)

    Thread.sleep(1000) // avoid exceeding tumblr rate limit

    val httpResponse = httpClient.execute(httpPost)
    val statusLine = httpResponse.getStatusLine
    val statusCode = statusLine.getStatusCode
    val result = statusCode match {
      case 201 =>
        val c = new BufferedHttpEntity(httpResponse.getEntity).getContent
        "Success! The new post ID is %s".format(
          Source.fromInputStream(c).mkString)
      case 403 => "Bad email or password"
      case _ => "Error: %s".format(statusLine)
    }
    println(result)
  }

  def getCaption(file: File) = {
    val caption = file.stripExtension
    caption.replace("_", " ")
  }

  def getDate(file: File) = getExifDate(file) match {
    case Some(date) => date
    case None => getFileDate(file)
  }

  def getExifDate(f: File) = {
    val file = new java.io.File(f.path)
    val meta = ImageMetadataReader.readMetadata(file);
    val exifDirectory = meta.getDirectory(classOf[ExifDirectory])
    exifDirectory.containsTag(ExifDirectory.TAG_DATETIME) match {
      case true => Some(exifDirectory.getDate(ExifDirectory.TAG_DATETIME))
      case _ => None
    }
  }

  def getFileDate(file: File) = file.name.startsWith("day") match {
      case true =>
        // add num of days extracted from file name to 2005/05/02.
        // e.g., day02_birth1.jpg => 2005/05/02 + 2days = 2005/05/04
        val cal = Calendar.getInstance
        cal.clear
        cal.set(Calendar.YEAR, 2005)
        cal.set(Calendar.MONTH, Calendar.MAY)
        cal.set(Calendar.DAY_OF_MONTH, 2)
        cal.add(Calendar.DAY_OF_MONTH, file.name.split("_")(0).substring(3).toInt)
        cal.getTime
      case false =>
        // set date extracted from the name of the file or directory
        val datePattern = """(\d{4})""".r
        val year = file.parent.parent.name
        val firstWord = file.name.split("_")(0)
        firstWord match {
          case datePattern(firstWord) =>
            val sdf = new SimpleDateFormat("yyyyMMdd")
            sdf.parse("%s%s".format(year, firstWord))
          case _ =>
            val month = file.parent.name
            val sdf = new SimpleDateFormat("yyyyMM")
            month.length match {
              case 2 => sdf.parse("%s%s".format(year, month))
              case 6 => sdf.parse(month)
            }
        }
  }
}
