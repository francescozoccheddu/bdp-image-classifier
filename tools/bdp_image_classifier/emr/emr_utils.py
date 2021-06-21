from ..utils.launcher import dont_run
from ..utils import cli
dont_run()


class Mode(cli.Choice):
    S3 = 'S3'
    SSH = 'SSH'


class InstanceType(cli.Choice):
    m4_large = 'm4.large'
    c5_xlarge = 'c5.xlarge'


def add_argparse_args(parser):
    parser.add_argument('--aws-ak-id', help='the AWS access key ID (use "AWS_ACCESS_KEY_ID" env variable if not provided)')
    parser.add_argument('--aws-ak-secret', help='the AWS secret access key (use "AWS_SECRET_ACCESS_KEY" env variable if not provided)')
    parser.add_argument('--aws-region', default='us-east-1', help='the AWS region to use')
    parser.add_argument('-m', '--mode', type=Mode, choices=Mode.choices(), default=Mode.SSH, help='the communication mode')
    parser.add_argument('-t', '--instance-type', type=InstanceType, choices=InstanceType.choices(), default=InstanceType.m4_large, help='the EC2 instance type')
    parser.add_argument('-c', '--instance-count', type=cli.make_int_arg(1, 10), default=1, help='the EC2 instance count')
    parser.add_argument('-o', '--output-dir', type=cli.output_dir_arg, default='results', help='the output directory')
    parser.add_argument('--suppress-ssh-output', action='store_true', help='suppress the app output when launched in SSH mode')
    parser.add_argument('--no-warning', action='store_true', help='disable the UAYOR warning')
    cli.add_argparse_quiet(parser)


def get_args(parser):
    args = parser.parse_args()
    cli.set_exception_hook()
    cli.set_logging(not args.quiet)
    if (args.aws_ak_id is None) != (args.aws_ak_secret is None):
        parser.error('you must provide both the --aws-ak-id and the --aws-ak-secret arguments or none of them')
    if args.aws_ak_id is None:
        import os
        args.aws_ak_id = os.environ.get('AWS_ACCESS_KEY_ID')
        args.aws_ak_secret = os.environ.get('AWS_SECRET_ACCESS_KEY')
        if (args.aws_ak_id is None) or (args.aws_ak_secret is None):
            parser.error('you must set the "AWS_ACCESS_KEY_ID" and the "AWS_SECRET_ACCESS_KEY" environment variables or provide the --aws-ak-id and the --aws-ak-secret arguments')
    if not args.no_warning:
        cli.err('------------------------------')
        msg = 'AWS will charge you for the EMR cluster'
        if args.mode == Mode.S3:
            msg += ' and the S3 bucket'
        cli.err(msg + '.')
        cli.err('This script should automatically terminate all allocated resources on exit, but bugs happen.')
        cli.err('Carefully check this script output and make sure that no AWS resources are active after its termination (by checking on the AWS console, for instance).')
        cli.err('I am not responsible for any unexpected charge.')
        cli.err('USE THIS SCRIPT AT YOUR OWN RISK.')
        cli.err('(use the --no-warning flag to suppress this message)')
        cli.err('------------------------------')
        cli.pause()
    return args
