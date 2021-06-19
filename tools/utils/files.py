from contextlib import contextmanager
import os
import shutil
from .launcher import dont_run
from .cli import is_logging
dont_run()


@contextmanager
def cwd(dir):
    abs_dir = abs(dir)
    abs_old_dir = abs(os.getcwd())
    os.chdir(abs_dir)
    try:
        yield
    finally:
        os.chdir(abs_old_dir)


def delete(path):
    abs_path = abs(path)
    if is_file(abs_path):
        os.remove(abs_path)
        return True
    elif is_dir(abs_path):
        shutil.rmtree(abs_path)
        return True
    else:
        return False


def delete_output_dir(dir, children=[], created=False):
    abs_dir = abs(dir)
    if is_dir(abs_dir):
        if created:
            delete(abs_dir)
            return True
        else:
            for child in children:
                res_child = join(abs_dir, child)
                delete(res_child)
            if len(children(abs_dir)) == 0:
                delete(abs_dir)
                return True
            else:
                return False
    elif is_file(abs_dir):
        raise RuntimeError('Not a directory')
    else:
        return False


def create_dir(dir, wipe=False):
    if is_dir(dir):
        if wipe:
            for file in children(dir):
                delete(file)
        return False
    elif is_file(dir):
        raise RuntimeError('Not a directory')
    else:
        os.mkdir(dir)
        return True


@contextmanager
def output_dir(dir, children=[], wipe=False):
    abs_dir = abs(dir)
    if wipe:
        for child in children:
            res_child = join(abs_dir, child)
            delete(res_child)
    created = create_dir(abs_dir)
    try:
        with cwd(abs_dir):
            yield
    except BaseException:
        delete_output_dir(abs_dir, children, created)
        raise


def temp_path(suffix=''):
    import tempfile
    return tempfile.mktemp(suffix, 'francescozoccheddu-bdp-image-classifier')


def download(url, output_file=None, msg='Downloading', show_progress=is_logging()):
    try:
        import requests
        buffer_size = 1024
        response = requests.get(url, stream=True)
        file_size = int(response.headers.get('Content-Length', 0))
        iterable = response.iter_content(buffer_size)
        if show_progress:
            from tqdm import tqdm
            progress = tqdm(response.iter_content(buffer_size), msg, total=file_size, unit='B', unit_scale=True, unit_divisor=1000)
            iterable = progress.iterable
        if output_file is None:
            import cgi
            header = response.headers.get('Content-Disposition', '')
            _, params = cgi.parse_header(header)
            filename = params.get('filename', '')
            ext = os.path.splitext(filename)[1]
            output_file = temp_path(ext)
        with open(output_file, 'wb') as f:
            for data in iterable:
                f.write(data)
                if show_progress:
                    progress.update(len(data))
        return output_file
    except BaseException:
        try_delete_file(output_file)
        raise


def try_delete_file(file):
    if is_file(file):
        try:
            delete(file)
        except Exception:
            pass


def extract(archive_file, output_dir, format=None, unwrap=False):
    if unwrap:
        temp_dir = temp_path()
        try:
            shutil.unpack_archive(archive_file, temp_dir, format)
            create_dir(output_dir)
            content = children(temp_dir)
            if len(content) > 1:
                raise RuntimeError('Multiple files in archive')
            if len(content) < 1:
                raise RuntimeError('Empty archive')
            wrapped = join(temp_dir, content[0])
            for child in children(wrapped):
                move(join(wrapped, child), output_dir)
        finally:
            delete(temp_dir)
    else:
        shutil.unpack_archive(archive_file, output_dir, format)


def download_and_extract(url, output_dir, format=None, unwrap=False, msg='Downloading', show_progress=is_logging()):
    file = download(url, None, msg, show_progress)
    try:
        extract(file, output_dir, format, unwrap)
    finally:
        try_delete_file(file)


def is_dir_writable(dir):
    if exists(dir):
        return is_dir(dir) and os.access(dir, os.W_OK)
    else:
        parent_dir = parent(dir) or '.'
        return os.access(parent_dir, os.W_OK)


def is_file_writable(file):
    if exists(file):
        return is_file(file) and os.access(file, os.W_OK)
    else:
        parent_dir = parent(file) or '.'
        return os.access(parent_dir, os.W_OK)


def is_file_readable(file):
    return is_file(file) and os.access(file, os.R_OK)


def is_dir_readable(dir):
    return is_dir(dir) and os.access(dir, os.R_OK)


def parent(path):
    return os.path.dirname(path)


def children(dir):
    return os.listdir(dir)


def is_dir(path):
    return os.path.isdir(path)


def is_file(path):
    return os.path.isfile(path)


def exists(path):
    return os.path.exists(path)


def read(file):
    with open(file) as f:
        return f.read()


def write(file, cnt):
    with open(file, 'w') as f:
        return f.write(cnt)


def append(file, cnt):
    with open(file, 'a') as f:
        return f.write(cnt)


def get_home():
    from pathlib import Path
    return str(Path.home())


def copy(src, dst):
    shutil.copyfile(src, dst)


def move(src, dst):
    shutil.move(src, dst)


def join(a, *args):
    return os.path.join(a, *args)


def abs(path):
    return os.path.abspath(path)


def rel(path):
    return os.path.relpath(path)


def name(path):
    return os.path.basename(path)


def isabs(path):
    return os.path.isabs(path)


def create_dir_tree(dir):
    return os.makedirs(dir, exist_ok=True)


def set_permissions(path, permission, recursive=False):
    if recursive and is_file(path):
        for root, dirs, files in os.walk(dir):
            for f in files + dirs:
                os.chmod(join(root, f), permission)
    else:
        os.chmod(path, permission)


def filter_file_lines(file, filter):
    with open(file, "r+") as f:
        lines = f.readlines()
        f.seek(0)
        for line in lines:
            if filter(line):
                f.write(line)
        f.truncate()
