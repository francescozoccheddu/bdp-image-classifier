
def download(url: str, output_file: str = None, silent: bool = False) -> str:
	import requests
	buffer_size = 1024
	response = requests.get(url, stream=True)
	file_size = int(response.headers.get('Content-Length', 0))
	iterable = response.iter_content(buffer_size)
	progress = None
	if output_file is None:
		output_file = get_temp_path()
	if not silent:
		try:
			from tqdm import tqdm
			progress = tqdm(response.iter_content(buffer_size), 'Downloading', total=file_size, unit='B', unit_scale=True, unit_divisor=1024)
			iterable = progress.iterable
		except:
			pass
	with open(output_file, 'wb') as f:
		for data in iterable:
			f.write(data)
			if progress is not None:
				progress.update(len(data))
	return output_file

def get_temp_path() -> str:
	import tempfile
	return tempfile.mktemp('francescozoccheddu-bdp-image-classifier')

def extract(archive_file: str, output_dir: str, format: str = None) -> None:
	import shutil
	shutil.unpack_archive(archive_file, output_dir, format)

def is_dir_writable(dir: str) -> bool:
	import os
	if os.path.exists(dir):
		return os.path.isdir(dir) and os.access(dir, os.W_OK)
	else:
		parent_dir = os.path.dirname(dir) or '.'
		return os.access(parent_dir, os.W_OK)

def is_file_writable(file: str) -> bool:
	import os
	if os.path.exists(file):
		return os.path.isfile(file) and os.access(file, os.W_OK)
	else:
		parent_dir = os.path.dirname(file) or '.'
		return os.access(parent_dir, os.W_OK)

def is_file_readable(file: str) -> bool:
	import os
	return os.path.isfile(file) and os.access(file, os.R_OK)

def is_dir_readable(dir: str) -> bool:
	import os
	return os.path.isdir(dir) and os.access(dir, os.R_OK)

def output_file_arg(arg: str) -> str:
	if not is_file_writable(arg):
		import argparse
		raise argparse.ArgumentTypeError('not a writable file')
	return arg

def output_dir_arg(arg: str) -> str:
	if not is_dir_writable(arg):
		import argparse
		raise argparse.ArgumentTypeError('not a writable dir')
	return arg

def input_file_arg(arg: str) -> str:
	if not is_file_readable(arg):
		import argparse
		raise argparse.ArgumentTypeError('not a readable file')
	return arg

def input_dir_arg(arg: str) -> str:
	if not is_dir_readable(arg):
		import argparse
		raise argparse.ArgumentTypeError('not a readable dir')
	return arg