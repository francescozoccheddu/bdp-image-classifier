
def _require_python():
    import sys
    min_version = (3, 5)
    if sys.version_info < min_version:
        sys.exit('Python %s.%s or later is required.\n' % min_version)


_require_python()

if __name__ == '__main__':
    import sys
    sys.exit('You should not directly run this script. See README.md for help.')


def download(url, output_file=None, silent=False):
    import requests
    buffer_size = 1024
    try:
        response = requests.get(url, stream=True)
        file_size = int(response.headers.get('Content-Length', 0))
        iterable = response.iter_content(buffer_size)
    except BaseException:
        raise DownloadFailedError()
    progress = None
    if not silent:
        try:
            from tqdm import tqdm
        except BaseException:
            print(f'Downloading {_humanize_size(file_size)}. Please wait...')
        else:
            progress = tqdm(response.iter_content(buffer_size), 'Downloading', total=file_size, unit='B', unit_scale=True, unit_divisor=1000)
            iterable = progress.iterable
    if output_file is None:
        output_file = get_temp_path()
    try:
        with open(output_file, 'wb') as f:
            for data in iterable:
                f.write(data)
                if progress is not None:
                    progress.update(len(data))
    except BaseException:
        raise DownloadFailedError()
    return output_file


def _humanize_size(size):
    for unit in ['', 'K', 'M']:
        if abs(size) < 1000.0:
            return '%3.1f%sB' % (size, unit)
        size /= 1000.0
    return '%.1f%sB' % (size, 'G')


def get_temp_path():
    import tempfile
    try:
        return tempfile.mktemp('francescozoccheddu-bdp-image-classifier')
    except BaseException:
        raise NoTempPathError()


def extract(archive_file, output_dir, format=None):
    import shutil
    try:
        shutil.unpack_archive(archive_file, output_dir, format)
    except BaseException:
        raise ExtractionFailedError()


def is_dir_writable(dir):
    import os
    if os.path.exists(dir):
        return os.path.isdir(dir) and os.access(dir, os.W_OK)
    else:
        parent_dir = os.path.dirname(dir) or '.'
        return os.access(parent_dir, os.W_OK)


def is_file_writable(file):
    import os
    if os.path.exists(file):
        return os.path.isfile(file) and os.access(file, os.W_OK)
    else:
        parent_dir = os.path.dirname(file) or '.'
        return os.access(parent_dir, os.W_OK)


def is_file_readable(file):
    import os
    return os.path.isfile(file) and os.access(file, os.R_OK)


def is_dir_readable(dir):
    import os
    return os.path.isdir(dir) and os.access(dir, os.R_OK)


def output_file_arg(arg):
    if not is_file_writable(arg):
        import argparse
        raise argparse.ArgumentTypeError('not a writable file')
    return arg


def output_dir_arg(arg):
    if not is_dir_writable(arg):
        import argparse
        raise argparse.ArgumentTypeError('not a writable dir')
    return arg


def input_file_arg(arg):
    if not is_file_readable(arg):
        import argparse
        raise argparse.ArgumentTypeError('not a readable file')
    return arg


def input_dir_arg(arg):
    if not is_dir_readable(arg):
        import argparse
        raise argparse.ArgumentTypeError('not a readable dir')
    return arg


class LoggableError(Exception):

    def __init__(self, message):
        super().__init__()
        self._message = message

    def log(self):
        printerr(self._message)


class DownloadFailedError(LoggableError):

    def __init__(self):
        super().__init__('Download failed.')


class ExtractionFailedError(LoggableError):

    def __init__(self):
        super().__init__('Extraction failed.')


class NoTempPathError(LoggableError):

    def __init__(self):
        super().__init__('Failed to get a temporary path.')


def printerr(message):
    import sys
    print(message, file=sys.stderr)


def hook_exceptions():
    import sys

    def except_hook(type, value, traceback):
        if issubclass(type, LoggableError):
            value.log()
        if issubclass(type, KeyboardInterrupt):
            printerr("Cancelled by user.")
        else:
            message = str(value).strip()
            if message:
                if message.count('\n') > 0:
                    printerr(f'Unhandled exception:\n{message}')
                else:
                    printerr(f'Unhandled exception: {message}')
            else:
                printerr('Unhandled exception.')

    sys.excepthook = except_hook
