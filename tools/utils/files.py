from contextlib import contextmanager
from .exceptions import FileSystemError, NetworkError
import os
import shutil
from .launcher import dont_run
dont_run()


class PathAccessError(FileSystemError):

    def __init__(self, path, path_type='path', cause=None):
        super().__init__(f'Cannot access {path_type} "{os.path.abspath(path)}"', cause, path, path_type)
        self._path_type = path_type
        self._path = path

    @property
    def path_type(self):
        return self._path_type

    @property
    def path(self):
        return self._path


class DirectoryAccessError(PathAccessError):

    def __init__(self, dir, cause=None):
        super().__init__(dir, 'directory', cause)


class FileAccessError(PathAccessError):

    def __init__(self, file, cause=None):
        super().__init__(file, 'file', cause)


class ExtractionError(FileSystemError):

    def __init__(self, archive_file, output_dir, cause=None):
        super().__init__(f'Extraction failed', cause, archive_file, output_dir)
        self._archive_file = archive_file
        self._output_dir = output_dir

    @property
    def archive_file(self):
        return self._archive_file

    @property
    def output_dir(self):
        return self._output_dir


class DownloadError(NetworkError):

    def __init__(self, url, output_file, cause=None):
        super().__init__('Download failed', cause, url, output_file)
        self._url = url
        self._output_file = output_file

    @property
    def url(self):
        return self._url

    @property
    def output_file(self):
        return self._output_file


@contextmanager
def cwd(dir):
    try:
        abs_dir = os.path.abspath(dir)
        abs_old_dir = os.path.abspath(os.getcwd())
        os.chdir(abs_dir)
    except OSError as exc:
        raise DirectoryAccessError(abs_dir, exc)
    try:
        yield
    finally:
        try:
            os.chdir(abs_old_dir)
        except OSError as exc:
            raise DirectoryAccessError(abs_old_dir, exc)


def delete(path):
    abs_path = os.path.abspath(path)
    if os.path.isfile(abs_path):
        try:
            os.remove(abs_path)
            return True
        except IOError as exc:
            raise FileAccessError(abs_path, cause=exc)
    elif os.path.isdir(abs_path):
        try:
            shutil.rmtree(abs_path)
            return True
        except IOError as exc:
            raise FileAccessError(abs_path, cause=exc)
    else:
        return False


def delete_output_dir(dir, children=[], created=False):
    abs_dir = os.path.abspath(dir)
    if os.path.isdir(abs_dir):
        if created:
            delete(abs_dir)
            return True
        else:
            for child in children:
                res_child = os.path.join(abs_dir, child)
                delete(res_child)
            if len(os.listdir(abs_dir)) == 0:
                delete(abs_dir)
                return True
            else:
                return False
    elif os.path.isfile(abs_dir):
        raise DirectoryAccessError(abs_dir, 'not a directory')
    else:
        return False


def create_dir(dir):
    if os.path.isdir(dir):
        return False
    elif os.path.isfile(dir):
        raise DirectoryAccessError(dir, 'not a directory')
    else:
        try:
            os.mkdir(dir)
            return True
        except Exception as exc:
            raise DirectoryAccessError(dir, exc)


@contextmanager
def output_dir(dir, children=[], wipe=False):
    if wipe:
        delete_output_dir(dir)
    created = create_dir(dir)
    try:
        with cwd(dir):
            yield
    except BaseException:
        delete_output_dir(dir, children, created)
        raise


def temp_path(suffix=None):
    import tempfile
    try:
        return tempfile.mktemp(suffix, 'francescozoccheddu-bdp-image-classifier')
    except Exception:
        raise FileSystemError('Failed to get a temporary path')


def download(url, output_file=None, silent=False):
    try:
        import requests
        buffer_size = 1024
        try:
            response = requests.get(url, stream=True)
            file_size = int(response.headers.get('Content-Length', 0))
            iterable = response.iter_content(buffer_size)
        except Exception as exc:
            raise DownloadError(url, output_file, exc)
        if not silent:
            from tqdm import tqdm
            progress = tqdm(response.iter_content(buffer_size), 'Downloading', total=file_size, unit='B', unit_scale=True, unit_divisor=1000)
            iterable = progress.iterable
        if output_file is None:
            import cgi
            from os import path
            header = response.headers.get('Content-Disposition', '')
            _, params = cgi.parse_header(header)
            filename = params.get('filename', '')
            ext = path.splitext(filename)[1]
            output_file = temp_path(ext)
        try:
            with open(output_file, 'wb') as f:
                for data in iterable:
                    f.write(data)
                    if not silent:
                        progress.update(len(data))
        except Exception as exc:
            raise DownloadError(url, output_file, exc)
        return output_file
    except BaseException:
        try_delete_file(output_file)
        raise

def try_delete_file(file):
    if os.path.isfile(file):
        try:
            delete(file)
        except:
            pass

def extract(archive_file, output_dir, format=None):
    import shutil
    try:
        shutil.unpack_archive(archive_file, output_dir, format)
    except Exception as exc:
        raise ExtractionError(archive_file, output_dir, exc)


def download_and_extract(url, output_dir, format=None):
    file = download(url)
    try:
        extract(file, output_dir, format)
    finally:
        try_delete_file(file)


def is_dir_writable(dir):
    if os.path.exists(dir):
        return os.path.isdir(dir) and os.access(dir, os.W_OK)
    else:
        parent_dir = os.path.dirname(dir) or '.'
        return os.access(parent_dir, os.W_OK)


def is_file_writable(file):
    if os.path.exists(file):
        return os.path.isfile(file) and os.access(file, os.W_OK)
    else:
        parent_dir = os.path.dirname(file) or '.'
        return os.access(parent_dir, os.W_OK)


def is_file_readable(file):
    return os.path.isfile(file) and os.access(file, os.R_OK)


def is_dir_readable(dir):
    return os.path.isdir(dir) and os.access(dir, os.R_OK)
