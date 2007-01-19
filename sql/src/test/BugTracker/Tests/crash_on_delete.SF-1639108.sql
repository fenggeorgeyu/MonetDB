set debug=32768;
start transaction;
create table number (value integer primary key);
insert into number (value) select 1;
select 'the following query causes the message "missing head type 2: var" in the server. i don''t know if that matters.';
insert into number (value) select 1+value from number;
insert into number (value) select 2+value from number;
insert into number (value) select 4+value from number;
insert into number (value) select 8+value from number;
insert into number (value) select 16+value from number;
insert into number (value) select 32+value from number;
insert into number (value) select 64+value from number;
insert into number (value) select 128+value from number;
insert into number (value) select 256+value from number;
insert into number (value) select 512+value from number;
insert into number (value) select 1024+value from number;
delete from number where value=1;
select 'the following query causes the server to crash printing "sql_optimize.mx:1011: set2pivot: Assertion `n'' failed."';
delete from number where exists (select true from number as factor1,number as factor2 where number.value=factor1.value*factor2.value);
select count(*) from number;
drop table number;
commit;
