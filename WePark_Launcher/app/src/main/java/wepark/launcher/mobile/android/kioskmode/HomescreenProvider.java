package wepark.launcher.mobile.android.kioskmode;


/**
 * Created by Saurabh on 11/3/2016.
 */
import edu.mit.mobile.android.content.GenericDBHelper;
import edu.mit.mobile.android.content.SimpleContentProvider;

public class HomescreenProvider extends SimpleContentProvider {

	public static final String AUTHORITY = "wepark.launcher.mobile.android.kioskmode";

	private static final int DB_VERSION = 1;

	public HomescreenProvider() {
		super(AUTHORITY, DB_VERSION);
	}

	@Override
	public boolean onCreate() {
		super.onCreate();

		final GenericDBHelper items = new GenericDBHelper(LauncherItem.class);
		addDirAndItemUri(items, LauncherItem.PATH);

		return true;
	}
}
