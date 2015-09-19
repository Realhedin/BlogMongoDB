package course;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import org.bson.Document;

import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.List;

public class BlogPostDAO {
    MongoCollection<Document> postsCollection;

    public BlogPostDAO(final MongoDatabase blogDatabase) {
        postsCollection = blogDatabase.getCollection("posts");
    }

    // Return a single post corresponding to a permalink
    public Document findByPermalink(String permalink) {

        Document post;
        post = postsCollection.find().filter(Filters.eq("permalink", permalink)).first();

        // fix up if a post has no likes
        if (post != null) {
            List<Document> comments = (List<Document>) post.get("comments");
            for (Document comment : comments) {
                if (!comment.containsKey("num_likes")) {
                    comment.put("num_likes", 0);
                }
            }
        }
        return post;
    }

    // Return a list of posts in descending order. Limit determines
    // how many posts are returned.
    public List<Document> findByDateDescending(int limit) {

        // Return a list of DBObjects, each one a post from the posts collection
        List<Document> posts;
        posts = postsCollection.find().sort(Sorts.descending("date")).limit(limit).into(new ArrayList<Document>());

        return posts;
    }

    public List<Document> findByTagDateDescending(final String tag) {
        return postsCollection.find(Filters.eq("tags", tag))
                .sort(Sorts.descending("date"))
                .limit(10)
                .into(new ArrayList<Document>());
    }

    public String addPost(String title, String body, List tags, String username) {

        System.out.println("inserting blog entry " + title + " " + body);

        String permalink = title.replaceAll("\\s", "_"); // whitespace becomes _
        permalink = permalink.replaceAll("\\W", ""); // get rid of non alphanumeric
        permalink = permalink.toLowerCase();

        // Build the post object and insert it
        Document post = new Document();
        post.append("title",title)
                .append("author",username)
                .append("body",body)
                .append("permalink",permalink)
                .append("tags",tags)
                .append("comments",new ArrayList<String>());
        post.append("date", GregorianCalendar.getInstance().getTime());

        postsCollection.insertOne(post);

        return permalink;
    }



    // Append a comment to a blog post
    public void addPostComment(final String name, final String email, final String body,
                               final String permalink) {
        Document doc = new Document();
        doc.append("author",name)
                .append("body",body);
        if (email != null && !email.equals("")) {
            doc.append("email",email);
        }

        postsCollection.updateOne(Filters.eq("permalink", permalink), new Document("$push", new Document("comments",doc)));
        System.out.println();
    }

    public void likePost(final String permalink, final int ordinal) {
       Document post =  findByPermalink(permalink);

        List comments = (ArrayList) post.get("comments");

        Document comment = (Document) comments.get(ordinal);

        Integer numOfLikes = (Integer) comment.get("num_likes");

        postsCollection.updateOne(Filters.eq("permalink", permalink),
                new Document("$set", new Document("comments."+ordinal+".num_likes", numOfLikes+1)));
    }
}

