from .launcher import dont_run
dont_run()


class LoggableError(Exception):

    @staticmethod
    def prefix(prefix, cause):
        from .cli import concat, humanize_exc, at_end
        cause = cause or ''
        if isinstance(cause, Exception):
            cause = humanize_exc(cause)
        if cause:
            cause = at_end(cause, '.')
        return concat(prefix, cause, sep=':', empty_sep='.')

    def __init__(self, message, cause=None, *info):
        super().__init__(message, cause, *info)
        self._message = LoggableError.prefix(message, cause)
        self._cause = cause

    @property
    def cause(self):
        return self._cause

    @property
    def message(self):
        return self._message


class AuthenticationError(LoggableError):
    pass


class FileSystemError(LoggableError):
    pass


class NetworkError(LoggableError):
    pass


class AuthenticationError(LoggableError):
    pass
