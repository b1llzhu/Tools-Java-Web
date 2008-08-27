/*
* Copyright 2004 The Apache Software Foundation.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*      http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

org_apache_myfaces_DateFormatSymbols = function()
{
        this.eras = new Array('BC', 'AD');
        this.months = new Array('January', 'February', 'March', 'April',
                'May', 'June', 'July', 'August', 'September', 'October',
                'November', 'December', 'Undecimber');
        this.shortMonths = new Array('Jan', 'Feb', 'Mar', 'Apr',
                'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct',
                'Nov', 'Dec', 'Und');
        this.weekdays = new Array('Sunday', 'Monday', 'Tuesday',
                'Wednesday', 'Thursday', 'Friday', 'Saturday');
        this.shortWeekdays = new Array('Sun', 'Mon', 'Tue',
                'Wed', 'Thu', 'Fri', 'Sat');
        this.ampms = new Array('AM', 'PM');
        this.zoneStrings = new Array(new Array(0, 'long-name', 'short-name'));
        var threshold = new Date();
        threshold.setYear(threshold.getYear()-80);
        this.twoDigitYearStart = threshold;
}

org_apache_myfaces_SimpleDateFormatParserContext = function()
{
        this.newIndex=0;
        this.retValue=0;
        this.year=0;
        this.ambigousYear=false;
        this.month=0;
        this.day=0;
        this.dayOfWeek=0;
        this.hour=0;
        this.min=0;
        this.sec=0;
        this.ampm=0;
        this.dateStr="";
}

org_apache_myfaces_SimpleDateFormat = function(pattern, dateFormatSymbols)
{
        this.pattern = pattern;
        this.dateFormatSymbols = dateFormatSymbols ? dateFormatSymbols :
                new org_apache_myfaces_DateFormatSymbols();
}
org_apache_myfaces_SimpleDateFormat.prototype._handle = function(dateStr, date, parse)
    {
        var patternIndex = 0;
        var dateIndex = 0;
        var commentMode = false;
        var lastChar = 0;
        var currentChar=0;
        var nextChar=0;
        var patternSub = null;

        var context = new org_apache_myfaces_SimpleDateFormatParserContext();

        if(date != null)
        {
            context.year = this._fullYearFromDate(date.getYear());
            context.month = date.getMonth();
            context.day = date.getDate();
            context.dayOfWeek = date.getDay();
            context.hour = date.getHours();
            context.min = date.getMinutes();
            context.sec = date.getSeconds();
        }

        while (patternIndex < this.pattern.length)
        {
            currentChar = this.pattern.charAt(patternIndex);

            if(patternIndex<(this.pattern.length-1))
            {
                nextChar = this.pattern.charAt(patternIndex+1);
            }
            else
            {
                nextChar = 0;
            }


            if (currentChar == '\'' && lastChar!='\\')
            {
                commentMode = !commentMode;
                patternIndex++;
            }
            else
            {
                if(!commentMode)
                {
                    if (currentChar == '\\' && lastChar!='\\')
                    {
                        patternIndex++;
                    }
                    else
                    {
                        if(patternSub == null)
                            patternSub = "";

                        patternSub+=currentChar;

                        if(currentChar != nextChar)
                        {
                            this._handlePatternSub(context, patternSub,
                                    dateStr, dateIndex, parse);

                            patternSub = null;

                            if(context.newIndex<0)
                                break;

                            dateIndex = context.newIndex;
                        }

                        patternIndex++;
                    }
                }
                else
                {
                    if(parse)
                    {
                        if(this.pattern.charAt(patternIndex)!=dateStr.charAt(dateIndex))
                        {
                            //invalid character in dateString
                            return null;
                        }
                    }
                    else
                    {
                        context.dateStr+=this.pattern.charAt(patternIndex);
                    }

                    patternIndex++;
                    dateIndex++;
                }
            }

            lastChar = currentChar;
        }

        this._handlePatternSub(context, patternSub,
                dateStr, dateIndex, parse);

        return context;
    };

org_apache_myfaces_SimpleDateFormat.prototype.parse = function(dateStr)
    {
        if(!dateStr || dateStr.length==0)
            return null;

        var context = this._handle(dateStr, null, true);

        if(context.retValue==-1)
            return null;

        this._adjustTwoDigitYear(context);

        return this._createDateFromContext(context);
    };
org_apache_myfaces_SimpleDateFormat.prototype._createDateFromContext=function(context)
    {
        return new Date(context.year, context.month,
                context.day,context.hour,context.min,context.sec);
    };
org_apache_myfaces_SimpleDateFormat.prototype.format = function(date)
    {
        var context = this._handle(null, date, false);

        return context.dateStr;
    };

org_apache_myfaces_SimpleDateFormat.prototype._parseString = function(context, dateStr, dateIndex, strings)
    {
        var fragment = dateStr.substr(dateIndex);
        var index = this._prefixOf(strings, fragment);
        if (index != -1) {
          context.retValue = index;
          context.newIndex = dateIndex + strings[index].length;
          return context;
        }

        context.retValue=-1;
        context.newIndex=-1;
        return context;
    };

org_apache_myfaces_SimpleDateFormat.prototype._parseNum = function(context, dateStr, posCount, dateIndex)
    {
        for(var i=Math.min(posCount,dateStr.length-dateIndex);i>0;i--)
        {
            var numStr = dateStr.substring(dateIndex,dateIndex+i);

            context.retValue = this._parseInt(numStr);

            if(context.retValue == -1)
                continue;

            context.newIndex = dateIndex+i;
            return context;
        }

        context.retValue = -1;
        context.newIndex = -1;
        return context;
    };

org_apache_myfaces_SimpleDateFormat.prototype._handlePatternSub = function(context, patternSub, dateStr, dateIndex, parse)
    {
        if(patternSub==null || patternSub.length==0)
            return;

        if(patternSub.charAt(0)=='y')
        {
            if(parse)
            {
                /* XXX @Arvid: whatever we do, we need to try to parse
                    the full year format - length means nothing for
                    parsing, only for formatting, so says SimpleDateFormat javadoc.
                    only if we run into problems as there are no separator chars, we
                    should use exact length parsing - how are we going to handle this?

                    Additionally, the threshold was not quite correct - it needs to
                    be set to current date - 80years...

                    this is done after parsing now!

                if (patternSub.length <= 3) {
                  this._parseNum(context, dateStr,2,dateIndex);
                  context.year = (context.retValue < 26)
                      ? 2000 + context.retValue : 1900 + context.retValue;
                } else {
                  this._parseNum(context, dateStr,4,dateIndex);
                  context.year = context.retValue;
                }*/
                this._parseNum(context, dateStr,4,dateIndex);

                if((context.newIndex-dateIndex)<4)
                {
                    context.year = context.retValue+1900;
                    context.ambigousYear = true;
                }
                else
                {
                    context.year = context.retValue;

                }
            }
            else
            {
                this._formatNum(context,context.year,patternSub.length <= 3 ? 2 : 4,true);
            }
        }
        else if(patternSub.charAt(0)=='M')
        {
            if(parse)
            {
              if (patternSub.length == 3) {
                var fragment = dateStr.substr(dateIndex, 3);
                var index = this._indexOf(this.dateFormatSymbols.shortMonths, fragment);
                if (index != -1) {
                  context.month = index;
                  context.newIndex = dateIndex + 3;
                }
              } else if (patternSub.length >= 4) {
                var fragment = dateStr.substr(dateIndex);
                var index = this._prefixOf(this.dateFormatSymbols.months, fragment);
                if (index != -1) {
                  context.month = index;
                  context.newIndex = dateIndex + this.dateFormatSymbols.months[index].length;
                }
              } else {
                this._parseNum(context, dateStr,2,dateIndex);
                context.month = context.retValue-1;
              }
            }
            else
            {
                if (patternSub.length == 3) {
                  context.dateStr += this.dateFormatSymbols.shortMonths[context.month];
                } else if (patternSub.length >= 4) {
                  context.dateStr += this.dateFormatSymbols.months[context.month];
                } else {
                  this._formatNum(context,context.month+1,patternSub.length);
                }
            }
        }
        else if(patternSub.charAt(0)=='d')
        {
            if(parse)
            {
                this._parseNum(context, dateStr,2,dateIndex);
                context.day = context.retValue;
            }
            else
            {
                this._formatNum(context,context.day,patternSub.length);
            }
        }
        else if(patternSub.charAt(0)=='E')
        {
            if(parse)
            {
              // XXX dayOfWeek is not used to generate date at the moment
              if (patternSub.length <= 3) {
                var fragment = dateStr.substr(dateIndex, 3);
                var index = this._indexOf(this.dateFormatSymbols.shortWeekdays, fragment);
                if (index != -1) {
                  context.dayOfWeek = index;
                  context.newIndex = dateIndex + 3;
                }
              } else {
                var fragment = dateStr.substr(dateIndex);
                var index = this._prefixOf(this.dateFormatSymbols.weekdays, fragment);
                if (index != -1) {
                  context.dayOfWeek = index;
                  context.newIndex = dateIndex + this.dateFormatSymbols.weekdays[index].length;
                }
              }
            }
            else
            {
              if (patternSub.length <= 3) {
                context.dateStr += this.dateFormatSymbols.shortWeekdays[context.dayOfWeek];
              } else {
                context.dateStr += this.dateFormatSymbols.weekdays[context.dayOfWeek];
              }
            }
        }
        else if(patternSub.charAt(0)=='H' ||
                patternSub.charAt(0)=='h')
        {
            if(parse)
            {
                this._parseNum(context, dateStr,2,dateIndex);
                context.hour = context.retValue;
            }
            else
            {
                this._formatNum(context,context.hour,patternSub.length);
            }
        }
        else if(patternSub.charAt(0)=='m')
        {
            if(parse)
            {
                this._parseNum(context, dateStr,2,dateIndex);
                context.min = context.retValue;
            }
            else
            {
                this._formatNum(context,context.min,patternSub.length);
            }
        }
        else if(patternSub.charAt(0)=='s')
        {
            if(parse)
            {
                this._parseNum(context, dateStr,2,dateIndex);
                context.sec = context.retValue;
            }
            else
            {
                this._formatNum(context,context.sec,patternSub.length);
            }
        }
        else if(patternSub.charAt(0)=='a')
        {
            if(parse)
            {
                this._parseString(context, dateStr,dateIndex,this.dateFormatSymbols.ampms);
                context.ampm = context.retValue;
            }
            else
            {
                context.dateStr += this.dateFormatSymbols.ampms[context.ampm];
            }
        }
        else
        {
            if(parse)
            {
                context.newIndex=dateIndex+patternSub.length;
            }
            else
            {
                context.dateStr+=patternSub;
            }

        }
    };

org_apache_myfaces_SimpleDateFormat.prototype._formatNum = function (context, num, length, ensureLength)
    {
        var str = num+"";

        while(str.length<length)
            str="0"+str;

        // XXX do we have to distinguish left and right 'cutting'
        //ensureLength - enable cutting only for parameters like the year, the other
        if (ensureLength && str.length > length) {
          str = str.substr(str.length - length);
        }

        context.dateStr+=str;
    };

    // perhaps add to Array.prototype
org_apache_myfaces_SimpleDateFormat.prototype._indexOf = function (array, value)
    {
      for (var i = 0; i < array.length; ++i) {
        if (array[i] == value) {
          return i;
        }
      }
      return -1;
    };

org_apache_myfaces_SimpleDateFormat.prototype._prefixOf = function (array, value)
    {
      for (var i = 0; i < array.length; ++i) {
        if (value.indexOf(array[i]) == 0) {
          return i;
        }
      }
      return -1;
    };

org_apache_myfaces_SimpleDateFormat.prototype._parseInt = function(value)
    {
        var sum = 0;

        for(var i=0;i<value.length;i++)
        {
            var c = value.charAt(i);

            if(c<'0'||c>'9')
            {
                return -1;
            }
            sum = sum*10+(c-'0');
        }

        return sum;
    };
org_apache_myfaces_SimpleDateFormat.prototype._fullYearFromDate = function(year)
    {

        var yearStr = year+"";

        if (yearStr.length < 4)
        {
            return year+1900;
        }

        return year;
    };
org_apache_myfaces_SimpleDateFormat.prototype._adjustTwoDigitYear = function(context)
    {

        if(context.ambigousYear)
        {
            var date = this._createDateFromContext(context);
            var threshold = this.dateFormatSymbols.twoDigitYearStart;

            if(date.getTime()<threshold.getTime())
                context.year += 100;
        }
    };

