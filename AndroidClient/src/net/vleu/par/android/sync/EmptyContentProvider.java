/*
 * Copyright ©2011 Brice Arnould
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package net.vleu.par.android.sync;

import net.jcip.annotations.ThreadSafe;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;

/**
 * It seems necessary to have a Content Provider in order to propose a sync
 * service. Even if it does nothing.
 * Please do not use it.
 */
/*
 * Note: it has android:multiprocess set to true precisely because it has no
 * state. If it were to have one, this value might need to be changed.
 */
@ThreadSafe
public final class EmptyContentProvider extends ContentProvider {

    @Override
    public int delete(final Uri uri, final String selection,
            final String[] selectionArgs) {
        throw new IllegalArgumentException("Unsupported URI: " + uri);
    }

    @Override
    public String getType(final Uri uri) {
        throw new IllegalArgumentException("Unsupported URI: " + uri);
    }

    @Override
    public Uri insert(final Uri uri, final ContentValues values) {
        throw new IllegalArgumentException("Unsupported URI: " + uri);
    }

    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public Cursor query(final Uri uri, final String[] projection,
            final String selection, final String[] selectionArgs,
            final String sortOrder) {
        return null;
    }

    @Override
    public int update(final Uri uri, final ContentValues values,
            final String selection, final String[] selectionArgs) {
        throw new IllegalArgumentException("Unsupported URI: " + uri);
    }

}
