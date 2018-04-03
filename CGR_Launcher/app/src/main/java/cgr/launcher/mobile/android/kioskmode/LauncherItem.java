package cgr.launcher.mobile.android.kioskmode;


/**
 * Created by Saurabh on 11/3/2016.
 */
import android.net.Uri;
import edu.mit.mobile.android.content.ContentItem;
import edu.mit.mobile.android.content.ProviderUtils;
import edu.mit.mobile.android.content.UriPath;
import edu.mit.mobile.android.content.column.DBColumn;
import edu.mit.mobile.android.content.column.TextColumn;

@UriPath(LauncherItem.PATH)
public class LauncherItem implements ContentItem {

	@DBColumn(type = TextColumn.class)
	public static final String PACKAGE_NAME = "package";

	@DBColumn(type = TextColumn.class)
	public static final String ACTIVITY_NAME = "activity";

	public static final String PATH = "apps";

	public static Uri CONTENT_URI = ProviderUtils.toContentUri(HomescreenProvider.AUTHORITY, PATH);
}

