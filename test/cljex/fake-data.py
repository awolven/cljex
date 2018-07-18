import random
from faker import Faker
fake=Faker()

out=open("test-data.txt", "w+")

color_list = ['red', 'orange', 'yellow', 'green', 'blue', 'purple']

def write_fake_date():
    date = fake.date()
    date_list = date.split('-')
    out.write(str(int(date_list[1])))
    out.write('/')
    out.write(str(int(date_list[2])))
    out.write('/')
    out.write(date_list[0])

def female_record():
    out.write(fake.last_name())
    out.write(" | ")
    out.write(fake.first_name_female())
    out.write(" | female | ")
    out.write(fake.word(ext_word_list=color_list))
    out.write(" | ")
    write_fake_date()
    out.write('\n')

def male_record():
    out.write(fake.last_name())
    out.write(" | ")
    out.write(fake.first_name_male())
    out.write(" | male | ")
    out.write(fake.word(ext_word_list=color_list))
    out.write(" | ")
    write_fake_date()
    out.write('\n')
    
for x in range(50):
    if (round(random.random()) == 0):
        female_record()
    else:
        male_record()
    
