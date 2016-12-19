/**
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 *
 */

package autosaveworld.features.backup.dropbox;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import autosaveworld.features.backup.utils.virtualfilesystem.VirtualFileSystem;
import autosaveworld.utils.StringUtils;
import autosaveworld.zlibs.com.dropbox.core.DbxClient;
import autosaveworld.zlibs.com.dropbox.core.DbxEntry;
import autosaveworld.zlibs.com.dropbox.core.DbxException;
import autosaveworld.zlibs.com.dropbox.core.DbxStreamWriter;
import autosaveworld.zlibs.com.dropbox.core.DbxWriteMode;

public class DropboxVirtualFileSystem extends VirtualFileSystem {

	private DbxClient dbxclient;
	private ArrayList<String> currentpath = new ArrayList<String>();
	public DropboxVirtualFileSystem(DbxClient dbxclient) {
		this.dbxclient = dbxclient;
	}

	@Override
	public void enterDirectory0(String dirname) throws IOException {
		currentpath.add(dirname);
	}

	@Override
	public void createDirectory0(String dirname) throws IOException {
		try {
			dbxclient.createFolder(getPath(dirname));
		} catch (DbxException e) {
			throw wrapException(e);
		}
	}

	@Override
	public void leaveDirectory() throws IOException {
        if (currentpath.isEmpty()) {
        	throw new IOException("Can't get parent directory of a root dir");
        }
        currentpath.remove(currentpath.size() - 1);
	}

	@Override
	public void deleteDirectoryRecursive(String dirname) throws IOException {
		delete(dirname);
	}

	@Override
	public boolean isDirectory(String dirname) throws IOException {
		try {
			return dbxclient.getMetadata(getPath(dirname)).isFolder();
		} catch (DbxException e) {
			throw wrapException(e);
		}
	}

	@Override
	public void deleteDirectory(String dirname) throws IOException {
		delete(dirname);
	}

	@Override
	public void deleteFile(String name) throws IOException {
		delete(name);
	}

	@Override
	protected Set<String> getEntries0() throws IOException {
		try {
			HashSet<String> files = new HashSet<String>();
			for (DbxEntry entry : dbxclient.getMetadataWithChildren(getPath(null)).children) {
				files.add(entry.name);
			}
			return files;
		} catch (DbxException e) {
			throw wrapException(e);
		}
	}

	@Override
	public void createFile(String name, InputStream inputsteam) throws IOException {
		try {
			dbxclient.uploadFileChunked(getPath(name), DbxWriteMode.force(), -1, new DbxStreamWriter.InputStreamCopier(inputsteam));
		} catch (DbxException e) {
			throw wrapException(e);
		}
	}


	private void delete(String name) throws IOException {
		try {
			dbxclient.delete(getPath(name));
		} catch (DbxException e) {
			throw wrapException(e);
		}
	}

	private String getPath(String name) {
		String path = '/' + StringUtils.join(currentpath, "/");
		if (!StringUtils.isNullOrEmpty(name)) {
			path += "/" + name;
		}
		return path;
	}


	static IOException wrapException(DbxException e) {
		return new IOException("Dbx error", e);
	}

}
