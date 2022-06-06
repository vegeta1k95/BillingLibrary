import zipfile
import glob
import os


def find_archives():
    return glob.glob('./*.zip')


def unpack_archive(archive):
    with zipfile.ZipFile(archive, "r") as f:
        for name in f.infolist():
            if "-1" in name.filename:
                name.filename = name.filename.replace("-1", "")
                f.extract(name, "./drawable-hdpi/")
            elif "-2" in name.filename:
                name.filename = name.filename.replace("-2", "")
                f.extract(name, "./drawable-xhdpi/")
            elif "-3" in name.filename:
                name.filename = name.filename.replace("-3", "")
                f.extract(name, "./drawable-xxhdpi/")
            elif "-4" in name.filename:
                name.filename = name.filename.replace("-4", "")
                f.extract(name, "./drawable-xxxhdpi/")
            else:
                f.extract(name, "./drawable-mdpi/")


for archive in find_archives():
    unpack_archive(archive)
